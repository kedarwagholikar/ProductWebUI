package synereo.client

import japgolly.scalajs.react.ReactDOM
import japgolly.scalajs.react.extra.router._
import japgolly.scalajs.react.vdom.prefix_<^._
import org.scalajs.dom
import synereo.client.components.{GlobalStyles, Icon}
import synereo.client.css.{SynereoCommanStylesCSS, AppCSS}
import synereo.client.models.UserModel
import synereo.client.modules._
import synereo.client.services.SYNEREOCircuit
import synereo.client.logger._

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport
import scalacss.Defaults._
import scalacss.ScalaCssReact._
import scalacss.mutable.GlobalRegistry

@JSExport("SYNEREOMain")
object SYNEREOMain extends js.JSApp {

  // Define the locations (pages) used in this application
  sealed trait Loc

  case object SynereoUserProfileViewLOC extends Loc

  case object SynereoLoc extends Loc

  case object SynereoDashboardLoc extends Loc

  case object SynereoTimelineViewLOC extends Loc

  case object SynereoBlogPostFullLOC extends Loc

  case object MarketPlaceFullLOC extends Loc

  // configure the router
  val routerConfig = RouterConfigDsl[Loc].buildConfig { dsl =>
    import dsl._
    (staticRoute(root, SynereoLoc) ~> renderR(ctl => Login(Login.Props()))
      | staticRoute("#synereologin", SynereoLoc) ~> renderR(ctl => Login(Login.Props()))
      | staticRoute("#synereodashboard", SynereoDashboardLoc) ~> renderR(ctl => Dashboard(ctl))
      | staticRoute("#synereofullblogpost", SynereoBlogPostFullLOC) ~> renderR(ctl => BlogPostFull(ctl))
      | staticRoute("#userprofileview", SynereoUserProfileViewLOC) ~> renderR(ctl => UserProfileView(ctl))
      | staticRoute("#usertimeline", SynereoTimelineViewLOC) ~> renderR(ctl => TimelineView(ctl))
      | staticRoute("#marketplacefull", MarketPlaceFullLOC) ~> renderR(ctl => MarketPlaceFull(ctl))
      ).notFound(redirectToPage(SynereoLoc)(Redirect.Replace))
  }.renderWith(layout)

  // base layout for all pages
  def layout(c: RouterCtl[Loc], r: Resolution[Loc]) = {
    <.div()(
      <.img(^.id := "loginLoader", SynereoCommanStylesCSS.Style.loading, ^.className := "hidden", ^.src := "./assets/images/processing.gif"),
      <.nav(^.id := "naviContainer", SynereoCommanStylesCSS.Style.naviContainer, ^.className := "navbar navbar-fixed-top")(
        <.div(^.className := "col-lg-1")(),
        <.div(^.className := "col-lg-10")(
          <.div(^.className := "navbar-header")(
            <.button(^.className := "navbar-toggle", "data-toggle".reactAttr := "collapse", "data-target".reactAttr := "#navi-collapse")(
              <.span(^.color := "white")(Icon.thList)
            ),
            c.link(SynereoLoc)(^.className := "navbar-header", <.img(SynereoCommanStylesCSS.Style.imgLogo, ^.src := "./assets/images/Synereo-logo-name.png"))
          ),
          <.div(^.id := "navi-collapse", ^.className := "collapse navbar-collapse")(
            SYNEREOCircuit.connect(_.user)(proxy => MainMenu(MainMenu.Props(c, r.page, proxy)))
          )
        ),
        <.div()()
      ),
      // the vertically center area
      r.render() /*,
      Footer(Footer.Props(c, r.page))*/
    )
  }

  @JSExport
  def main(): Unit = {
    log.warn("Application starting")
    // send log messages also to the server
    log.enableServerLogging("/logging")
    log.info("This message goes to server as well")
    // create stylesheet
    GlobalStyles.addToDocument()
    AppCSS.load
    GlobalRegistry.addToDocumentOnRegistration()
    // create the router
    val router = Router(BaseUrl(dom.window.location.href.takeWhile(_ != '#')), routerConfig)
    // tell React to render the router in the document body
    //ReactDOM.render(router(), dom.document.getElementById("root"))
    ReactDOM.render(router(), dom.document.getElementById("root"))
  }
}