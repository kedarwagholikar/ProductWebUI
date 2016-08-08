package client.modals

import shared.models.MessagePostContent
import client.services.LGCircuit
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.OnUnmount
import japgolly.scalajs.react.vdom.prefix_<^._
import client.components.Bootstrap._
import client.components.Icon.Icon
import client.components._
import client.css.{DashBoardCSS, ProjectCSS}
import client.handlers.PostData
import japgolly.scalajs.react
import scalacss.Defaults._
import scalacss.ScalaCssReact._
import scala.language.reflectiveCalls
import org.querki.jquery._
import shared.sessionitems.SessionItems
import scala.scalajs.js
import org.scalajs.dom.FileReader
import org.scalajs.dom.raw.UIEvent
import diode.AnyAction._

object NewMessage {
  @inline private def bss = GlobalStyles.bootstrapStyles

  case class Props(buttonName: String, addStyles: Seq[StyleA] = Seq(), addIcons: Icon, title: String)

  case class State(showNewMessageForm: Boolean = false)

  abstract class RxObserver[BS <: BackendScope[_, _]](scope: BS) extends OnUnmount {
  }

  class Backend(t: BackendScope[Props, State]) extends RxObserver(t) {
    def mounted(props: Props): Callback = {
      t.modState(s => s.copy(showNewMessageForm = true))
    }

    def addNewMessageForm(): Callback = {
      t.modState(s => s.copy(showNewMessageForm = true))
    }

    def addMessage(/*postMessage:PostMessage*/): Callback = {
      t.modState(s => s.copy(showNewMessageForm = false))
    }
  }

  val component = ReactComponentB[Props]("NewMessage")
    .initialState(State())
    .backend(new Backend(_))
    .renderPS(($, P, S) => {
      val B = $.backend
      <.div()(
        Button(Button.Props(B.addNewMessageForm(), CommonStyle.default, P.addStyles, P.addIcons, P.title, className = "profile-action-buttons"), P.buttonName),
        if (S.showNewMessageForm) NewMessageForm(NewMessageForm.Props(B.addMessage, "New Message"))
        else
          Seq.empty[ReactElement]
      )
    })
    .configure(OnUnmount.install)
    .build

  def apply(props: Props) = component(props)
}

// #todo think about better way for getting data from selectize input
// so that you don't have to pass the parentId explicitly
object NewMessageForm {

  val messageID: js.Object = "#messageID"

  // shorthand for styles
  @inline private def bss = GlobalStyles.bootstrapStyles

  case class Props(submitHandler: () => Callback, header: String)

  case class State(postMessage: MessagePostContent, postNewMessage: Boolean = false,
                   cnxsSelectizeParentId: String = "postNewMessageSelectizeInput", labelSelectizeParentId: String = "labelsSelectizeParent")

  case class Backend(t: BackendScope[Props, State]) {

    def hide: Callback = Callback {
      $(t.getDOMNode()).modal("hide")
    }

    def updateSubject(e: ReactEventI): react.Callback = {
      val value = e.target.value
      t.modState(s => s.copy(postMessage = s.postMessage.copy(subject = value)))
    }

    def updateContent(e: ReactEventI): react.Callback = {
      val value = e.target.value
      t.modState(s => s.copy(postMessage = s.postMessage.copy(text = value)))
    }


    def fromSelecize(): Callback = Callback {}

    def updateImgSrc(e: ReactEventI): react.Callback = Callback {
      val value = e.target.files.item(0)
      println("Img src = " + value)
      var reader = new FileReader()
      reader.onload = (e: UIEvent) => {
        val contents = reader.result.asInstanceOf[String]
        println(s"in on load $contents")
        t.modState(s => s.copy(postMessage = s.postMessage.copy(imgSrc = contents))).runNow()
      }
      reader.readAsDataURL(value)
    }

    def hideModal(): Unit = {
      $(t.getDOMNode()).modal("hide")
    }

    def mounted(): Callback = Callback {

    }

    def submitForm(e: ReactEventI): react.Callback = {
      e.preventDefault()
      val state = t.state.runNow()
      LGCircuit.dispatch(PostData(state.postMessage, Some(state.cnxsSelectizeParentId),
        SessionItems.MessagesViewItems.MESSAGES_SESSION_URI, Some(state.labelSelectizeParentId)))
      val messageID: js.Object = "#messageID"
      if ($(messageID).hasClass("disabled"))
        t.modState(s => s.copy(postNewMessage = false))
      else
        t.modState(s => s.copy(postNewMessage = true))

    }

    def formClosed(state: State, props: Props): Callback = {
      props.submitHandler()
    }

    // scalastyle:off
    def render(s: State, p: Props) = {
      val connectionsProxy = LGCircuit.connect(_.connections)
      val searchesProxy = LGCircuit.connect(_.searches)
      val headerText = p.header
      Modal(
        Modal.Props(
          // header contains a cancel button (X)
          header = hide => <.span(<.button(^.tpe := "button", bss.close, ^.onClick --> hide, Icon.close), <.div(DashBoardCSS.Style.modalHeaderText)(headerText)),
          // this is called after the modal has been hidden (animation is completed)
          closed = () => formClosed(s, p)
        ),

        <.form("data-toggle".reactAttr := "validator", ^.role := "form", ^.onSubmit ==> submitForm)(
          <.div(^.className := "row", DashBoardCSS.Style.MarginLeftchkproduct)(
            <.div(DashBoardCSS.Style.marginTop10px)(),
            <.div(^.id := s.cnxsSelectizeParentId)(
              connectionsProxy(connectionsProxy => ConnectionsSelectize(ConnectionsSelectize.Props(connectionsProxy, s.cnxsSelectizeParentId, fromSelecize)))
            ),
            <.div(DashBoardCSS.Style.paddingTop10px, ^.id := s.labelSelectizeParentId)(
              searchesProxy(searchesProxy => LabelsSelectize(LabelsSelectize.Props(searchesProxy, "labelsSelectizeParent")))
            ),
            <.div(DashBoardCSS.Style.paddingTop10px)(
              <.input(^.`type` := "file", ^.onChange ==> updateImgSrc),
              if (s.postMessage.imgSrc != "") {
                <.img(DashBoardCSS.Style.imgSize, ^.src := s.postMessage.imgSrc)
              } else {
                <.div("")
              }
            ),
            <.div(^.className := "form-group")(
              <.textarea(^.rows := 6, ^.placeholder := "Subject", bss.formControl, ^.className:="form-control",ProjectCSS.Style.textareaWidth, DashBoardCSS.Style.replyMarginTop, ^.value := s.postMessage.subject, ^.onChange ==> updateSubject, ^.required := true)
            ),
            <.div(^.className := "form-group")(
              <.textarea(^.rows := 6, ^.placeholder := "Enter your message here:", bss.formControl,^.className:="form-control", ProjectCSS.Style.textareaWidth, DashBoardCSS.Style.replyMarginTop, ^.value := s.postMessage.text, ^.onChange ==> updateContent, ^.required := true)
            )
          ),
          <.div()(
            <.div(DashBoardCSS.Style.modalHeaderPadding, ^.className := "text-right", ^.className := "form-group")(
              <.button(^.tpe := "submit", ^.id := "messageID", ^.className := "btn", DashBoardCSS.Style.btnDefault, DashBoardCSS.Style.marginLeftCloseBtn, "Send"),
              <.button(^.tpe := "button", ^.className := "btn", DashBoardCSS.Style.btnDefault, DashBoardCSS.Style.marginLeftCloseBtn, ^.onClick --> hide, "Cancel")
            )
          ),
          <.div(bss.modal.footer, DashBoardCSS.Style.marginTop10px, DashBoardCSS.Style.marginLeftRight)()
        )
      )
    }
  }

  private val component = ReactComponentB[Props]("PostNewMessage")
    .initialState_P(p => State(new MessagePostContent()))
    .renderBackend[Backend]
    .componentDidUpdate(scope => Callback {
      if (scope.currentState.postNewMessage) {
        scope.$.backend.hideModal
      }
    })
    .componentDidMount(scope => scope.backend.mounted())
    .build

  def apply(props: Props) = component(props)
}
