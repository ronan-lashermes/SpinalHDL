package spinal.idslplugin

// The `PostInitCallback` trait in the Scala compiler plugin is utilized to add additional steps to the initialization process of an object, specifically after the constructor has been called. 
// Any class that extends this trait will have its constructor calls modified to include an invocation to a `postInitCallback` method.
// This method is intended for executing tasks that can only be performed after an object's instantiation, allowing for more complex setup procedures that depend on runtime information.
// The specific use of the `postInitCallback` will depend on the implementation within the class extending this trait.
trait PostInitCallback {
    def postInitCallback(): this.type
}

// The `ValCallback` trait in the Scala compiler plugin is used to add custom behavior to `val` definitions within a class that uses this trait. 
// When such a class is compiled, each `val` that isn't a parameter accessor and doesn't have a `DontName` annotation will have its right-hand side replaced with a call to a `valCallback` method. 
// This method takes the original value of the `val` and its name as arguments. The exact nature of the custom behavior depends on the implementation of the `valCallback` method. 
// This functionality can serve purposes like logging, dynamic behavior based on `val` values, or error checking and validation.
trait ValCallback {
    def valCallback[T](ref: T, name: String): T
}
