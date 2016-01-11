package livelygig.client.modules

import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{Callback, ReactComponentB}
import livelygig.client.LGMain.Loc
import livelygig.client.LGMain.Loc
import livelygig.client.components.Icon
import livelygig.client.css._
import livelygig.client.modules.NewMessage

import scalacss.ScalaCssReact._

object MessagesPresets {
  // create the React component for Dashboard
  val component = ReactComponentB[RouterCtl[Loc]]("Messages")
    .render_P(ctl =>
      // todo: Need to parameterize.
      // This example is for Talent
      <.div(^.id:="middelNaviContainer",HeaderCSS.Style.middelNaviContainer)(
        <.div(^.className :="row")(
          <.div(^.className:="col-md-8 col-sm-8 col-xs-8")(
            <.div(^.className:="btn-group")(
              <.button(HeaderCSS.Style.projectCreateBtn, ^.className:="btn dropdown-toggle","data-toggle".reactAttr := "dropdown")("Unread Messages ")(
                <.span(^.className:="caret")
              ),
              <.ul(HeaderCSS.Style.dropdownMenuWidth, ^.className:="dropdown-menu")(
                <.li()(<.a(^.href:="#")("Recommended Matches")),
                <.li()(<.a(^.href:="#")("Favorited")),
                <.li()(<.a(^.href:="#")("Available")),
                <.li()(<.a(^.href:="#")("Active Unavailable")),
                <.li()(<.a(^.href:="#")("Inactive")),
                <.li()(<.a(^.href:="#")("Hidden")),
                <.li(^.className:="divider")(),
                <.li()(<.a(^.href:="#")("Videographers w/5+ yrs experience")),
                <.li()(<.a(^.href:="#")("Customize..."))
              )
            ),
            /*<.button(HeaderCSS.Style.createNewProjectBtn, ^.className:="btn")("New Message")()*/
              NewMessage(NewMessage.Props(ctl))
          ),

            <.div(^.className:="col-md-4 col-sm-8 col-xs-4")(
              <.div(FooterCSS.Style.footGlyphContainer)(
                <.div(MessagesCSS.Style.displayInline)(
                  <.a(FooterCSS.Style.displayInlineGlyph)(^.href:="https://github.com/LivelyGig", ^.target:="_blank", "data-toggle".reactAttr := "tooltip", "title".reactAttr :="GitHub" )(<.span()(Icon.github))),
                <.div(MessagesCSS.Style.displayInline)(
                  <.a(FooterCSS.Style.displayInlineGlyph)(^.href:="https://twitter.com/LivelyGig", ^.target:="_blank", "data-toggle".reactAttr := "tooltip", "title".reactAttr :="Twitter" )(
                    <.span()(Icon.twitter))),
                <.div(MessagesCSS.Style.displayInline)(
                  <.a(FooterCSS.Style.displayInlineGlyph)(^.href:="https://www.facebook.com/LivelyGig-835593343168571/", ^.target:="_blank", "data-toggle".reactAttr := "tooltip", "title".reactAttr :="Facebook" )(
                    <.span()(Icon.facebook))),
                <.div(MessagesCSS.Style.displayInline)(
                  <.a(FooterCSS.Style.displayInlineGlyph)(^.href:="https://plus.google.com/+LivelygigCommunity", ^.target:="_blank", "data-toggle".reactAttr := "tooltip", "title".reactAttr :="Google Plus" )(
                    <.span()(Icon.googlePlus))),
                <.div(MessagesCSS.Style.displayInline)(
                  <.a(FooterCSS.Style.displayInlineGlyph)(^.href:="https://www.youtube.com/channel/UCBM73EEC5disDCDnvUXMe4w", ^.target:="_blank", "data-toggle".reactAttr := "tooltip", "title".reactAttr :="YouTube Channel" )(
                    <.span()(Icon.youtube))),
                <.div(MessagesCSS.Style.displayInline)(
                  <.a(FooterCSS.Style.displayInlineGlyph)(^.href:="https://www.linkedin.com/company/10280853", ^.target:="_blank", "data-toggle".reactAttr := "tooltip", "title".reactAttr :="LinkedIn" )(
                    <.span()(Icon.linkedin))),
                <.div(MessagesCSS.Style.displayInline)(
                  <.a(FooterCSS.Style.displayInlineGlyph)(^.href:="https://livelygig.slack.com", ^.target:="_blank", "data-toggle".reactAttr := "tooltip", "title".reactAttr :="Slack" )(
                    <.span()(Icon.slack)))
              )
            )
          )
        )

    )
    .componentDidMount(scope => Callback {
    })
    .build
}