package synereo.client.modules

import diode.react.ReactPot._
import diode.react._
import diode.data.Pot
import japgolly.scalajs.react.{ReactElement, _}
import japgolly.scalajs.react.vdom.prefix_<^._
import org.querki.jquery._
import shared.models.{MessagePost, MessagePostContent}
import synereo.client.rootmodels.MessagesRootModel
import synereo.client.components._
import synereo.client.css.{DashboardCSS, SynereoCommanStylesCSS}
import synereo.client.modalpopups.{FullPostViewModal, NewMessageForm, ServerErrorModal, AmplifyPostForm}
import scalacss.ScalaCssReact._
import scala.scalajs.js
import synereo.client.components.Icon
import scala.language.reflectiveCalls
import org.widok.moment.Moment
import shared.dtos.{ApiResponse, ErrorResponse, SendAmpsResponse}
import synereo.client.logger
import synereo.client.services.{CoreApi, SYNEREOCircuit}
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global

//scalastyle:off
object Dashboard {

  case class Props(proxy: ModelProxy[Pot[MessagesRootModel]], loginLoader: String = "loginLoader")

  case class State(ShowFullPostView: Boolean = false,
                   preventFullPostView: Boolean = true,
                   showErrorModal: Boolean = false
                  )

  class DashboardBackend(t: BackendScope[Dashboard.Props, Dashboard.State]) {

    def serverError(): Callback = {
      t.modState(s => s.copy(showErrorModal = false))
    }
  }

  val component = ReactComponentB[Props]("Dashboard")
    .initialState_P(p => State())
    .backend(new DashboardBackend(_))
    .renderPS((t, props, state) => {
      val getServerError = SYNEREOCircuit.zoom(_.appRootModel).value
      <.div(^.id := "dashboardContainerMain", ^.className := "container-fluid")(
        <.div(^.className := "container-fluid", DashboardCSS.Style.homeFeedMainContainer)(
          <.div(^.className := "row")(
            <.div(^.className := "col-lg-12 col-md-12 col-sm-12 col-xs-12", DashboardCSS.Style.paddingLRZero)(
              <.div(
                props.proxy().renderFailed(ex => /*<.div(
                      //                      <.span(^.id := "loginLoader", SynereoCommanStylesCSS.Style.loading, ^.className := "", Icon.spinnerIconPulse),
                      <.div(SynereoCommanStylesCSS.Style.renderFailedMessage)(s"We are encountering problems with serving the request!${ex.getMessage}")
                    )*/
                  if (!getServerError.isServerError)
                    ServerErrorModal(ServerErrorModal.Props(t.backend.serverError))
                  else
                    <.span()
                ),
                props.proxy().render(
                  messagesRootModel =>
                    HomeFeedList(messagesRootModel.messagesModelList)
                ),
                props.proxy().renderPending(ex => <.div(
                  <.div(^.id := s"#${props.loginLoader}", SynereoCommanStylesCSS.Style.messagesLoadingWaitCursor, ^.className := "", Icon.spinnerIconPulse)
                )
                ),
                <.div(
                  if (props.proxy().isEmpty) {
                    <.div(^.id := s"#${props.loginLoader}", SynereoCommanStylesCSS.Style.messagesLoadingWaitCursor, ^.className := "", Icon.spinnerIconPulse)
                  } else {
                    <.span()
                  }
                )
              )
            )
          )
        )
      )
    })
    .build

  def apply(proxy: ModelProxy[Pot[MessagesRootModel]]) = component(Props(proxy))
}


object HomeFeedList {

  val SHOWLOVEPOST = "showLovePost"
  val SHOWCOMMENTPOST = "showCommentPost"
  val SHOWCIRCLEPOST = "showCirclePost"
  val SHOWSHAREPOST = "showSharePost"

  val rightPost: js.Object = "#rightPost"
  val FeedTimeOut = 1500
  var collapsiblePost = ""
  val searchesProxy = SYNEREOCircuit.connect(_.searches)

  case class Props(messages: Seq[MessagePost] /*, rightPost: (Boolean) => Callback*/)

  case class State(showFullPostView: Boolean = false,
                   showAmplifyPostForm: Boolean = false,
                   showForwardPostForm: Boolean = false,
                   messagePost: MessagePost = new MessagePost(postContent = new MessagePostContent()),
                   fromSender: String = "",
                   toReceiver: String = "",
                   senderAddress: String = "",
                   showLovePost: Boolean = false,
                   showCommentPost: Boolean = false,
                   showCirclePost: Boolean = false,
                   showSharePost: Boolean = false,
                   showReplyPostForm: Boolean = false
                  )

  class HomeFeedListBackend(t: BackendScope[HomeFeedList.Props, HomeFeedList.State]) {

    def amplifyPost(senderAddress: String): Callback = {
      t.modState(state => state.copy(showAmplifyPostForm = true, senderAddress = senderAddress))
    }

    def postAmplified(amount: String, to: String, isAmplified: Boolean): Callback = {
      logger.log.debug("postAmplified called ")
      if (isAmplified) {
        logger.log.debug(s"Sending $amount AMPs to $to")
        CoreApi.sendAmps(amount, to).onComplete {
          case Success(res) =>
            if(res.contains("OK")) logger.log.debug(s"$amount AMPs were successfully sent.")
            else logger.log.debug(s"Failed to parse the response on sending AMPs: $res.")
          case Failure(res) =>
            logger.log.debug(s"Sending AMPs failed: $res.")
        }
      }
      t.modState(s => s.copy(showAmplifyPostForm = false))
    }

    def forwardPost(message: MessagePost): Callback = {
      //      logger.log.debug("forwarding a Post")
      t.modState(state => state.copy(showForwardPostForm = true, messagePost = message))
    }

    def replyPost(message: MessagePost): Callback = {
      //      logger.log.debug("replying a Post")
      t.modState(state => state.copy(showReplyPostForm = true, messagePost = message))
    }

    def postForwarded(): Callback = {
      //      logger.log.debug("postForwarded")
      t.modState(state => state.copy(showForwardPostForm = false))
    }

    def postReplyed(): Callback = {
      //      logger.log.debug("postRepled")
      t.modState(state => state.copy(showReplyPostForm = false))
    }


    def closeFullViewModalPopUp(): Callback = {
      t.modState(s => s.copy(showFullPostView = false))
    }

    def openFullViewModalPopUP(message: MessagePost, fromSender: String, toReceiver: String): Callback = {
      t.modState(s => s.copy(showFullPostView = true, messagePost = message, fromSender = fromSender, toReceiver = toReceiver))
    }

    def showPeekView(message: MessagePost): Callback = {
      //      if ($(s"#home-feed-card-${message.uid}".asInstanceOf[js.Object]).find("#rightPost").hasClass("hidden")) {
      //        $(s"#home-feed-card-${message.uid}".asInstanceOf[js.Object]).find("#collapsePost").addClass("hidden")
      //        $(s"#home-feed-card-${message.uid}".asInstanceOf[js.Object]).find("#rightPost").removeClass("hidden")
      //        //$(rightPost).height($(s"#home-feed-card-${message.uid}".asInstanceOf[js.Object]).height() /*+   $(s"#collapse-post-${message.uid}".asInstanceOf[js.Object]).height()*/)
      //      } else {
      //        //$(rightPost).height($(s"#home-feed-card-${message.uid}".asInstanceOf[js.Object]).height() /* +   $(s"#collapse-post-${message.uid}".asInstanceOf[js.Object]).height()*/)
      //        $(s"#home-feed-card-${message.uid}".asInstanceOf[js.Object]).find("#rightPost").addClass("hidden")
      //        $(s"#home-feed-card-${message.uid}".asInstanceOf[js.Object]).find("#collapsePost").removeClass("hidden")
      //      }

      if ($(s"#home-feed-card-${message.uid}".asInstanceOf[js.Object]).find("#collapsePost").hasClass("hidden")) {
        $(s"#home-feed-card-${message.uid}".asInstanceOf[js.Object]).find("#collapsePost").removeClass("hidden")
      } else {
        $(s"#home-feed-card-${message.uid}".asInstanceOf[js.Object]).find("#collapsePost").addClass("hidden")
      }

      t.modState(s => s.copy(showFullPostView = false))
    }

    def showRightPost(getRightPost: String): Callback = {
      val state = t.state.runNow()
      getRightPost match {
        case SHOWLOVEPOST => {
          if (state.showLovePost)
            t.modState(s => s.copy(showLovePost = false, showSharePost = false, showCirclePost = false, showCommentPost = false))
          else
            t.modState(s => s.copy(showLovePost = true, showSharePost = false, showCirclePost = false, showCommentPost = false))
        }
        case SHOWCOMMENTPOST => {
          if (state.showCommentPost)
            t.modState(s => s.copy(showCommentPost = false, showSharePost = false, showCirclePost = false, showLovePost = false))
          else
            t.modState(s => s.copy(showCommentPost = true, showSharePost = false, showCirclePost = false, showLovePost = false))
        }
        case SHOWCIRCLEPOST => {
          if (state.showCirclePost)
            t.modState(s => s.copy(showCirclePost = false, showCommentPost = false, showLovePost = false, showSharePost = false))
          else
            t.modState(s => s.copy(showCirclePost = true, showCommentPost = false, showLovePost = false, showSharePost = false))
        }
        case SHOWSHAREPOST => {
          if (state.showSharePost)
            t.modState(s => s.copy(showSharePost = false, showCirclePost = false, showCommentPost = false, showLovePost = false))
          else
            t.modState(s => s.copy(showSharePost = true, showCirclePost = false, showCommentPost = false, showLovePost = false))
        }
        case _ => {
          t.modState(s => s.copy(showSharePost = false, showCirclePost = false, showCommentPost = false, showLovePost = false))
        }
      }
    }
  }

  val MessagesList = ReactComponentB[Props]("MessagesList")
    .initialState_P(p => State())
    .backend(new HomeFeedListBackend(_))
    .renderPS((t, props, state) => {
      def renderMessages(message: MessagePost) = {
        //println(s"In dashboard ${message.sender.imgSrc}")
        <.li(^.id := s"home-feed-card-${message.uid}", ^.className := "media", DashboardCSS.Style.CardHolderLiElement)(
          <.div(^.className := "row",SynereoCommanStylesCSS.Style.feedViewRow)(
            <.div(^.className := "col-md-3 col-sm-2",SynereoCommanStylesCSS.Style.feedViewPostLeftDiv)(
              ""
            ),
            <.div(^.className := "col-md-6 col-sm-8 showRightPost")(
              <.div(^.className := "card-shadow", DashboardCSS.Style.userPost)(
                <.div(^.className := "row")(
                  <.div(^.className := "col-md-1 pull-left")(
                    if (message.sender.connection.target.split("/")(2) == SYNEREOCircuit.zoom(_.sessionRootModel.sessionUri).value.split("/")(2)) {
                      <.img(^.className := "media-object", ^.src := SYNEREOCircuit.zoom(_.user).value.imgSrc, ^.alt := "user avatar", DashboardCSS.Style.homeFeedUserAvatar)
                    } else {
                      <.img(^.className := "media-object", ^.src := message.sender.imgSrc, ^.alt := "user avatar", DashboardCSS.Style.homeFeedUserAvatar)
                    }
                  ),
                  <.div(^.className := "col-md-11", SynereoCommanStylesCSS.Style.paddingLeftZero)(
                    <.div(DashboardCSS.Style.userNameDescription, ^.className := "pull-left")(
                      <.span(^.className := "fromSenderTooltip", "data-toggle".reactAttr := "tooltip", "title".reactAttr :=
                        (if (message.sender.name == "me")
                          message.sender.connection.source.split("/")(2)
                        else
                          message.sender.connection.target.split("/")(2)),
                        "data-placement".reactAttr := "right")(s"From  : ${message.sender.name}"),
                      <.div("data-toggle".reactAttr := "tooltip", "title".reactAttr := message.created, "data-placement".reactAttr := "right")(Moment(message.created).format("LLL").toLocaleString)
                    ),
                    <.div(DashboardCSS.Style.userNameDescription, SynereoCommanStylesCSS.Style.paddingLeft15p)(
                      <.span(s"To  : ${message.receivers.map(_.name).mkString(", ")}")
                    ),
                    <.button(^.className := "btn btn-default pull-right", DashboardCSS.Style.homeFeedCardBtn)(MIcon.moreVert),
                    if (message.sender.name.equals("me")) {
                      <.span()
                    } else {
                      <.button(^.className := "btn btn-default pull-right", DashboardCSS.Style.ampTokenBtn,
                        "data-toggle".reactAttr := "tooltip", "title".reactAttr := "Amplify Post", "data-placement".reactAttr := "right",
                        ^.onClick --> t.backend.amplifyPost(message.sender.connection.target.split("/")(2)))(
                        <.img(^.src := "./assets/synereo-images/amptoken.png", DashboardCSS.Style.ampTokenImg)
                      )
                    },
                    <.button(^.className := "btn btn-default pull-right", DashboardCSS.Style.ampTokenBtn,
                      "data-toggle".reactAttr := "tooltip", "title".reactAttr := "Forward Post", "data-placement".reactAttr := "right",
                      ^.onClick --> t.backend.forwardPost(message))(
                      <.span(Icon.mailForward)
                    ),
                    <.button(^.className := "btn btn-default pull-right", DashboardCSS.Style.ampTokenBtn,
                      "data-toggle".reactAttr := "tooltip", "title".reactAttr := "Reply Post", "data-placement".reactAttr := "right",
                      ^.onClick --> t.backend.replyPost(message))(
                      <.span(Icon.mailReply)
                    )
                  )
                ),

                <.div(^.className := "row")(
                  <.div(^.className := "col-md-12")(
                    if (message.postContent.imgSrc != "" /*&& message.postContent.imgSrc.size > 80659*/) {
                      <.div(
                        <.div(DashboardCSS.Style.cardPostImage)(
                          <.div(DashboardCSS.Style.cardImageContainer)(
                            <.img(^.src := message.postContent.imgSrc, ^.className := "img-responsive", DashboardCSS.Style.cardImage, ^.onClick --> t.backend.openFullViewModalPopUP(message, message.sender.name, message.receivers.map(_.name).mkString(", ")))
                          )
                        ),
                        <.div(DashboardCSS.Style.cardDescriptionContainerDiv, DashboardCSS.Style.cardPaddingBottom, ^.onClick --> t.backend.openFullViewModalPopUP(message, message.sender.name, message.receivers.map(_.name).mkString(", ")))(
                          <.h3(message.postContent.subject, DashboardCSS.Style.cardHeading),
                          <.div(^.id := "collapsePost", ^.className := "textOverflowPost", DashboardCSS.Style.cardText)(message.postContent.text),
                          <.div(^.id := s"collapse-post-${message.uid}", ^.className := "collapse", DashboardCSS.Style.cardText)(
                            message.postContent.text
                          )
                        )
                      )
                    }
                    else {
                      <.div(
                        <.div(DashboardCSS.Style.cardDescriptionContainerDiv, DashboardCSS.Style.cardPaddingBottom, ^.onClick --> t.backend.openFullViewModalPopUP(message, message.sender.name, message.receivers.map(_.name).mkString(", ")))(
                          <.h3(message.postContent.subject, DashboardCSS.Style.cardHeading),
                          <.div(^.id := "collapsePost", ^.className := "textOverflowPost", DashboardCSS.Style.cardText)(message.postContent.text),
                          <.div(^.id := s"collapse-post-${message.uid}", ^.className := "collapse", DashboardCSS.Style.cardText)(
                            message.postContent.text
                          )
                        )
                      )
                    })),
                <.div(DashboardCSS.Style.cardDescriptionContainerDiv)(
                  if (message.postContent.text.length > 105) {
                    <.button(SynereoCommanStylesCSS.Style.synereoBlueText, DashboardCSS.Style.homeFeedCardBtn,
                      "data-toggle".reactAttr := "collapse", "data-target".reactAttr := s"#collapse-post-${message.uid}", ^.className := "glance-view-button", ^.onClick --> t.backend.showPeekView(message))(
                      (MIcon.moreHoriz)
                    )
                  }
                  else {
                    Seq.empty[ReactElement]
                  }
                )
              )
            ),
            <.div(^.id := "rightPost", ^.className := "col-sm-2 ", SynereoCommanStylesCSS.Style.feedViewLftHeightPost)(
              <.div(^.id := "trans",^.className := "row",SynereoCommanStylesCSS.Style.feedViewRightAnimDivStatusIconRow)(
                <.div(^.className := "row", SynereoCommanStylesCSS.Style.marginTop20px)(
                  <.div(^.className := "col-md-2 col-xs-1",SynereoCommanStylesCSS.Style.feedViewRightAnimDivMainStatusIcon,
                    <.img(^.src := "./assets/synereo-images/Love.svg",SynereoCommanStylesCSS.Style.feedViewRightPostDivSubIcon,SynereoCommanStylesCSS.Style.animLove, ^.onClick --> t.backend.showRightPost("showLovePost"))
                  ),
                  if (state.showLovePost) // <.div(^.className := "col-md-8", SynereoCommanStylesCSS.Style.lovePost)()
                    <.div(
                      <.div(^.className := "col-md-2 col-xs-1",SynereoCommanStylesCSS.Style.feedViewRightAnimDivSubIconCol, DashboardCSS.Style.postDescription, "10"),
                      <.div(^.className := "col-md-2 col-xs-1",SynereoCommanStylesCSS.Style.feedViewRightAnimDivSubIconCol)(<.img(^.src := "./assets/synereo-images/AMP_FullColor+circle.svg",SynereoCommanStylesCSS.Style.feedViewRightPostDivSubIcon,SynereoCommanStylesCSS.Style.animSubIconFirst)),
                      <.div(^.className := "col-md-2 col-xs-1",SynereoCommanStylesCSS.Style.feedViewRightAnimDivSubIconCol)(<.img(^.src := "./assets/synereo-images/AMP_FullColor+circle.svg",SynereoCommanStylesCSS.Style.feedViewRightPostDivSubIcon,SynereoCommanStylesCSS.Style.animSubIconSecond)),
                      <.div(^.className := "col-md-2 col-xs-1",SynereoCommanStylesCSS.Style.feedViewRightAnimDivSubIconCol)(<.img(^.src := "./assets/synereo-images/AMP_FullColor+circle.svg",SynereoCommanStylesCSS.Style.feedViewRightPostDivSubIcon,SynereoCommanStylesCSS.Style.animSubIconThird)),
                      <.div(^.className := "col-md-2 col-xs-1",SynereoCommanStylesCSS.Style.feedViewRightAnimDivSubIconCol)(<.img(^.src := "./assets/synereo-images/AMP_FullColor+circle.svg",SynereoCommanStylesCSS.Style.feedViewRightPostDivSubIcon,SynereoCommanStylesCSS.Style.animSubIconFourth))
                    )
                  else Seq.empty[ReactElement]

                ),
                <.div(^.className := "row", /*if (!state.showLovePost)*/ SynereoCommanStylesCSS.Style.marginTop20px /* else ^.marginTop := "0.px"*/)(
                  <.div(^.className := "col-xs-2",SynereoCommanStylesCSS.Style.feedViewRightAnimDivMainStatusIcon)(<.img(^.src := "./assets/synereo-images/Comment.svg",SynereoCommanStylesCSS.Style.feedViewRightPostDivSubIcon,SynereoCommanStylesCSS.Style.animComment, ^.onClick --> t.backend.showRightPost("showCommentPost"))),
                  if (state.showCommentPost)
                    <.div(
                      <.div(^.className := "col-md-2 col-xs-1 ",SynereoCommanStylesCSS.Style.feedViewRightAnimDivSubIconCol, DashboardCSS.Style.postDescription, "10"),
                      <.div(^.className := "col-md-2 col-xs-1 ",SynereoCommanStylesCSS.Style.feedViewRightAnimDivSubIconCol)(<.img(^.src := "./assets/synereo-images/AMP_FullColor+circle.svg",SynereoCommanStylesCSS.Style.feedViewRightPostDivSubIcon,SynereoCommanStylesCSS.Style.animSubIconFirst)),
                      <.div(^.className := "col-md-2 col-xs-1",SynereoCommanStylesCSS.Style.feedViewRightAnimDivSubIconCol)(<.img(^.src := "./assets/synereo-images/AMP_FullColor+circle.svg",SynereoCommanStylesCSS.Style.feedViewRightPostDivSubIcon,SynereoCommanStylesCSS.Style.animSubIconSecond)),
                      <.div(^.className := "col-md-2 col-xs-1",SynereoCommanStylesCSS.Style.feedViewRightAnimDivSubIconCol)(<.img(^.src := "./assets/synereo-images/AMP_FullColor+circle.svg",SynereoCommanStylesCSS.Style.feedViewRightPostDivSubIcon,SynereoCommanStylesCSS.Style.animSubIconThird)),
                      <.div(^.className := "col-md-2 col-xs-1",SynereoCommanStylesCSS.Style.feedViewRightAnimDivSubIconCol)(<.img(^.src := "./assets/synereo-images/AMP_FullColor+circle.svg",SynereoCommanStylesCSS.Style.feedViewRightPostDivSubIcon,SynereoCommanStylesCSS.Style.animSubIconFourth))
                    )
                  else
                    Seq.empty[ReactElement]
                ),
                <.div(^.className := "row", SynereoCommanStylesCSS.Style.marginTop20px)(
                  <.div(^.className := "col-md-2 col-xs-1",SynereoCommanStylesCSS.Style.feedViewRightAnimDivMainStatusIcon)(<.img(^.src := "./assets/synereo-images/Amp_circle.gif",SynereoCommanStylesCSS.Style.feedViewRightPostDivSubIcon,SynereoCommanStylesCSS.Style.animAmp_Circle, ^.onClick --> t.backend.showRightPost("showCirclePost"))),
                  if (state.showCirclePost)
                    <.div(
                      <.div(^.className := "col-md-2 col-xs-1",SynereoCommanStylesCSS.Style.feedViewRightAnimDivSubIconCol, DashboardCSS.Style.postDescription, "12"),
                      <.div(^.className := "col-md-2 col-xs-1",SynereoCommanStylesCSS.Style.feedViewRightAnimDivSubIconCol)(<.img(^.src := "./assets/synereo-images/AMP_FullColor+circle.svg",SynereoCommanStylesCSS.Style.feedViewRightPostDivSubIcon,SynereoCommanStylesCSS.Style.animSubIconFirst)),
                      <.div(^.className := "col-md-2 col-xs-1",SynereoCommanStylesCSS.Style.feedViewRightAnimDivSubIconCol)(<.img(^.src := "./assets/synereo-images/AMP_FullColor+circle.svg",SynereoCommanStylesCSS.Style.feedViewRightPostDivSubIcon,SynereoCommanStylesCSS.Style.animSubIconSecond)),
                      <.div(^.className := "col-md-2 col-xs-1",SynereoCommanStylesCSS.Style.feedViewRightAnimDivSubIconCol)(<.img(^.src := "./assets/synereo-images/AMP_FullColor+circle.svg",SynereoCommanStylesCSS.Style.feedViewRightPostDivSubIcon,SynereoCommanStylesCSS.Style.animSubIconThird)),
                      <.div(^.className := "col-md-2 col-xs-1",SynereoCommanStylesCSS.Style.feedViewRightAnimDivSubIconCol)(<.img(^.src := "./assets/synereo-images/AMP_FullColor+circle.svg",SynereoCommanStylesCSS.Style.feedViewRightPostDivSubIcon,SynereoCommanStylesCSS.Style.animSubIconFourth))
                    )
                  else
                    Seq.empty[ReactElement]
                ),
                <.div(^.className := "row", SynereoCommanStylesCSS.Style.marginTop20px)(
                  <.div(^.className := "col-md-2 col-xs-1",SynereoCommanStylesCSS.Style.feedViewRightAnimDivMainStatusIcon)(<.img(^.src := "./assets/synereo-images/Share.svg",SynereoCommanStylesCSS.Style.feedViewRightPostDivSubIcon,SynereoCommanStylesCSS.Style.animShare, ^.onClick --> t.backend.showRightPost("showSharePost"))),
                  if (state.showSharePost)
                    <.div(
                      <.div(^.className := "col-md-2 col-xs-1",SynereoCommanStylesCSS.Style.feedViewRightAnimDivSubIconCol, DashboardCSS.Style.postDescription, "1"),
                      <.div(^.className := "col-md-2 col-xs-1",SynereoCommanStylesCSS.Style.feedViewRightAnimDivSubIconCol)(<.img(^.src := "./assets/synereo-images/AMP_FullColor+circle.svg",SynereoCommanStylesCSS.Style.feedViewRightPostDivSubIcon,SynereoCommanStylesCSS.Style.animSubIconFirst)),
                      <.div(^.className := "col-md-2 col-xs-1",SynereoCommanStylesCSS.Style.feedViewRightAnimDivSubIconCol)(<.img(^.src := "./assets/synereo-images/AMP_FullColor+circle.svg",SynereoCommanStylesCSS.Style.feedViewRightPostDivSubIcon,SynereoCommanStylesCSS.Style.animSubIconSecond)),
                      <.div(^.className := "col-md-2 col-xs-1",SynereoCommanStylesCSS.Style.feedViewRightAnimDivSubIconCol)(<.img(^.src := "./assets/synereo-images/AMP_FullColor+circle.svg",SynereoCommanStylesCSS.Style.feedViewRightPostDivSubIcon,SynereoCommanStylesCSS.Style.animSubIconThird)),
                      <.div(^.className := "col-md-2 col-xs-1",SynereoCommanStylesCSS.Style.feedViewRightAnimDivSubIconCol)(<.img(^.src := "./assets/synereo-images/AMP_FullColor+circle.svg",SynereoCommanStylesCSS.Style.feedViewRightPostDivSubIcon,SynereoCommanStylesCSS.Style.animSubIconFourth))
                    )
                  else
                    Seq.empty[ReactElement]
                )
              )
            )
          )
        )
      }
      <.div(
        <.div(
          if (state.showAmplifyPostForm) {
            AmplifyPostForm(AmplifyPostForm.Props(t.backend.postAmplified, state.senderAddress))
          } else if (state.showForwardPostForm) {
            searchesProxy(searchesProxy => NewMessageForm(NewMessageForm.Props(t.backend.postForwarded, "New Message", searchesProxy, state.messagePost,state.showReplyPostForm,state.showForwardPostForm)))
          } else if (state.showReplyPostForm) {
            searchesProxy(searchesProxy => NewMessageForm(NewMessageForm.Props(t.backend.postReplyed, "Reply", searchesProxy, state.messagePost, state.showReplyPostForm,state.showForwardPostForm)))
          } else if (state.showFullPostView) {
            FullPostViewModal(FullPostViewModal.Props(t.backend.closeFullViewModalPopUp, state.messagePost, state.fromSender, state.toReceiver))
          } else {
            Seq.empty[ReactElement]
          }
        ),
        //homeFeedMediaList id is important see synereo_app.js
        <.ul(^.id := "homeFeedMediaList", ^.className := "media-list ", DashboardCSS.Style.homeFeedContainer)(
          props.messages map renderMessages
        )
      )
    })
    .build

  def apply(messages: Seq[MessagePost]) = MessagesList(Props(messages))
}