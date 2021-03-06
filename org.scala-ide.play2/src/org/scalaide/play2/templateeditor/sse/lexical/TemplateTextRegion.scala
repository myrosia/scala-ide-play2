package org.scalaide.play2.templateeditor.sse.lexical

import org.scalaide.ui.syntax.ScalaSyntaxClass
import org.eclipse.wst.sse.core.internal.parser.ContextRegion

case class TemplateTextRegion(syntaxClass: ScalaSyntaxClass, newStart: Int, newTextLength: Int, newLength: Int)
  extends ContextRegion(syntaxClass.displayName, newStart, newTextLength, newLength)