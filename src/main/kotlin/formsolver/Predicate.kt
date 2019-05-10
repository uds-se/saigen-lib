package formsolver

class Predicate @JvmOverloads constructor(
    namespace2: String,
    nome: String,
    val isRangeElement: Boolean,
    numerosity: Int = 0
) : Element(namespace2, nome, numerosity)
