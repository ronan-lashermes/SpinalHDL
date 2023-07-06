package spinal.idslplugin.components

import scala.collection.mutable.ArrayBuffer
import scala.reflect.internal.Trees
import scala.tools.nsc.Global
import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.transform.Transform

// Define MainTransformer as a plugin component for the Scala compiler
class MainTransformer(val global: Global) extends PluginComponent with Transform {

    override val phaseName: String = "idsl-plugin" // Set the phase name of this compiler plugin

    // Define when this compiler plugin should run during the compilation process
    override val runsAfter: List[String] = List("uncurry")
    override val runsRightAfter: Option[String] = Some("uncurry")

    // The actual transformer object that will transform the abstract syntax tree (AST) during compilation
    override protected def newTransformer(unit: global.CompilationUnit): global.Transformer = ToStringMaskerTransformer

    import global._

    // Define the transformer
    object ToStringMaskerTransformer extends Transformer {

        // Helper function to check if a symbol has a specific annotation
        def symbolHasAnnotation(s: Symbol, name: String): Boolean = {
            if (s.annotations.exists(_.symbol.name.toString() == name)) return true
            s.parentSymbols.exists(symbolHasAnnotation(_, name))
        }

        // Helper function to check if a symbol or any of its parents has a specific trait
        def symbolHasTrait(s: Symbol, name: String): Boolean = {
            s.parentSymbols.exists { p =>
                (p.fullName == name) || symbolHasTrait(p, name)
            }
        }

        // Helper function to check if a type or any of its parent types has a specific trait
        def typeHasTrait(s: Type, name: String): Boolean = {
            s.parents.exists { p =>
                p.toString().toString == name  || typeHasTrait(p, name)
            }
        }

        // The main transformation function that operates on the AST
        override def transform(tree: global.Tree): global.Tree = {
            val transformedTree = super.transform(tree) // Transform the tree using the parent's transform function
            transformedTree match { // Match the transformed tree to specific cases
                case cd: ClassDef => { // Case for class definitions
                    var ret: Tree = cd

                    // Check if the class contains a val definition for io of type spinal.core.Bundle
                    val withIoBundle = cd.impl.body.exists{
                            case vd : ValDef if vd.name.toString == "io " && vd.rhs != null && typeHasTrait(vd.rhs.tpe, "spinal.core.Bundle") => true
                            case _ => false
                    }

                    // If the class contains io but does not extend spinal.core.Component or other specific traits, report a compilation error
                    if(withIoBundle && !symbolHasTrait(cd.symbol, "spinal.core.Component") && !symbolHasTrait(cd.symbol, "spinal.core.Area" ) && !symbolHasTrait(cd.symbol, "spinal.core.Data" ) && !symbolHasTrait(cd.symbol, "spinal.core.AllowIoBundle" )){
                            global.globalError(cd.symbol.pos, s"MISSING EXTENDS COMPONENT\nclass with 'val io = new Bundle{...}' should extends spinal.core.Component")
                    }

                    // If the class extends the trait spinal.idslplugin.ValCallback, manipulate the class body
                    if (symbolHasTrait(cd.symbol, "spinal.idslplugin.ValCallback")) {
                            val clazz = cd.impl.symbol.owner // Get the class symbol
                            val func = clazz.tpe.members.find(_.name.toString == "valCallback").get // Find the valCallback function
                            val body = cd.impl.body.map { // Map each element in the class body to a new element
                                case vd: ValDef if !vd.mods.isParamAccessor  && !vd.symbol.annotations.exists(_.symbol.name.toString == "DontName") && vd.rhs.nonEmpty =>
                                    // If the element is a val declaration, is not a parameter accessor, does not have the DontName annotation, and has a non-empty right hand side
                                    val nameStr = vd.getterName.toString // Get the name of the getter
                                    val const = Constant(nameStr) // Wrap it in a Constant
                                    val lit = Literal(const) // Wrap the Constant in a Literal
                                    val thiz = This(clazz) // Get the this reference for the class
                                    val sel = Select(thiz, func) // Create a Select tree for the valCallback function
                                    val appl = Apply(sel, List(vd.rhs, lit)) // Create an Apply tree for the valCallback function with the original rhs and the Literal as arguments

                                    // Set the types for the new trees
                                    thiz.tpe = clazz.tpe
                                    sel.tpe = func.tpe
                                    appl.tpe = definitions.UnitTpe
                                    lit.setType(definitions.StringTpe)

                                    // Replace the original val declaration with a new one that has the Apply tree as the rhs
                                    treeCopy.ValDef(vd, vd.mods, vd.name, vd.tpt, appl)

                                case e => e // For all other cases, just keep the tree element as is
                            }

                            val impl = treeCopy.Template(cd.impl, cd.impl.parents, cd.impl.self, body) // Create a new class body with the modified elements
                            val cdNew = treeCopy.ClassDef(cd, cd.mods, cd.name, cd.tparams, impl) // Create a new class definition with the new class body

                            ret = cdNew // Set ret to the new class definition
                    }

                    ret // Return the modified class definition
                }
                case a: Apply => { // Case for Apply trees
                    var ret: Tree = a // Initialize ret

                    if (a.fun.symbol.isConstructor) { // If the Apply is a constructor call
                        val sym = a.fun.symbol.enclClass // Get the enclosing class of the constructor
                        val tpe = sym.typeOfThis // Get the type of this for the enclosing class
                        if (symbolHasTrait(sym, "spinal.idslplugin.PostInitCallback")) { // If the enclosing class extends the trait spinal.idslplugin.PostInitCallback
                            val avoidIt = a match { // Check if the Apply is a constructor call for a superclass or for the current class
                                case Apply(Select(Super(_, _), _), _) => true
                                case Apply(Select(This(_), _), _) => true
                                case _ => false
                            }
                            if (!avoidIt) { // If the Apply is not a constructor call for a superclass or for the current class
                                val func = tpe.members.find(_.name.toString == "postInitCallback").get // Find the postInitCallback function
                                val sel = Select(a, func.name) // Create a Select tree for the postInitCallback function
                                val appl = Apply(sel, Nil) // Create an Apply tree for the postInitCallback function with no arguments

                                // Set the types for the new trees
                                sel.tpe = MethodType(Nil, a.tpe)
                                appl.tpe = a.tpe

                                ret = appl // Set ret to the new Apply tree
                            }
                        }
                    }
                    ret // Return the modified Apply tree
                }
                case oth => { // Case for other types of trees
                    transformedTree // Just return the tree as it was transformed by the parent's transform function
                }
            }
        }
    }
}
