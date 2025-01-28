
package io.github.tsgen.typescript.generator.compiler;

import io.github.tsgen.typescript.generator.parser.Model;


public interface ModelTransformer {

    public Model transformModel(SymbolTable symbolTable, Model model);

}
