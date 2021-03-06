package org.scalaide.play2.routeeditor.hyperlink

import org.scalaide.core.compiler.InteractiveCompilationUnit
import org.scalaide.core.IScalaProject
import org.eclipse.jdt.core.IJavaElement
import org.eclipse.jface.text.IDocument
import org.eclipse.jface.text.IRegion
import org.eclipse.jface.text.hyperlink.IHyperlink
import org.scalaide.play2.routeeditor.RouteAction

object RouteHyperlinkComputer {

  /** Return the hyperlink to controller referenced in this route, if it exists.
   */
  def detectHyperlinks(scalaProject: IScalaProject, document: IDocument, region: IRegion, createJavaHyperlink: (RouteAction, IJavaElement) => IHyperlink): Option[IHyperlink] = {
    RouteAction.routeActionAt(document, region.getOffset()).flatMap {
      routeAction =>
        val routeActionParamTypes= routeAction.params.map(_._2)
        val scalaHyperlink: Option[Option[IHyperlink]] = scalaProject.presentationCompiler { compiler =>
          import compiler._
          import org.scalaide.core.compiler.IScalaPresentationCompiler.Implicits._
          asyncExec {

            // label for the hyperlink
            def methodLabel(method: Symbol): String = {
              method.fullNameString + method.paramss.map(_.map(_.tpe.toLongString).mkString("(", ",", ")")).mkString
            }

            // filter based on the method parameter types.
            def parametersMatch(method: Symbol): Boolean = {
              method.paramss match {
                case Nil =>
                  routeAction.params == Nil
                case firstSet :: Nil =>
                  val a = firstSet.map(_.tpe.toLongString)
                  a == routeActionParamTypes
                case _ =>
                  false // doesn't support multiple parameter list in the route file
              }
            }

            // object with the given name
            val obj = rootMirror.getModuleIfDefined(routeAction.typeName)

            // method or methods of the object with the given name
            val objMethod = obj.info.member(newTermName(routeAction.methodName))

            // if the method of the object doesn't exist, than perhaps routeAction.typeName is actually a class
            // so let's try getting the class and searching it's methods for routeAction.methodName
            val method =
              if (objMethod.exists) objMethod
              else rootMirror.getClassIfDefined(routeAction.typeName).info.member(newTermName(routeAction.methodName))

            if (!method.exists) {
              None
            } else {
              // find the method with the right parameter types
              val filteredMethod = if (method.isOverloaded) {
                method.alternatives.find(parametersMatch(_))
              } else {
                Some(method).find(parametersMatch(_))
              }
              // generate the IHyperlink if it is a Scala method.
              // For Java method, the computation is delayed outside of the presentation compiler thread.
              val res: Option[Option[IHyperlink]] = filteredMethod.map {
                method =>
                  if (method.isJavaDefined) {
                    None
                  } else {
                    compiler.mkHyperlink(method, "Open Declaration", routeAction.region, scalaProject.javaProject, methodLabel _)
                  }
              }
              res
            }
          }.getOption().flatten
        }.flatten

        scalaHyperlink.flatMap {
          _ match {
            case None =>
              // a result but in Java, not in Scala
              val javaElements=
                (new MethodFinder(scalaProject.javaProject)).searchMethod(routeAction.fullName, routeAction.params.map(_._2).toArray)

              javaElements.headOption.map {
                createJavaHyperlink(routeAction, _)
              }
            case Some(hyperlink) =>
              // the scala hyperlink
              Some(hyperlink)
          }
        }

    }

  }

}