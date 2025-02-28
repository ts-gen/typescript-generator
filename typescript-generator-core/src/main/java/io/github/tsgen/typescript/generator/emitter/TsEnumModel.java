
package io.github.tsgen.typescript.generator.emitter;

import io.github.tsgen.typescript.generator.compiler.EnumKind;
import io.github.tsgen.typescript.generator.compiler.EnumMemberModel;
import io.github.tsgen.typescript.generator.compiler.Symbol;
import io.github.tsgen.typescript.generator.parser.EnumModel;
import java.util.List;
import java.util.Objects;


public class TsEnumModel extends TsDeclarationModel {

    private final EnumKind kind;
    private final List<EnumMemberModel> members;
    private final boolean isNonConstEnum;

    public TsEnumModel(Class<?> origin, Symbol name, EnumKind kind, List<EnumMemberModel> members, List<String> comments, boolean isNonConstEnum) {
        super(origin, null, name, comments);
        this.kind = Objects.requireNonNull(kind);
        this.members = Objects.requireNonNull(members);
        this.isNonConstEnum = isNonConstEnum;
    }

    public static TsEnumModel fromEnumModel(Symbol name, EnumModel enumModel, boolean isNonConstEnum) {
        return new TsEnumModel(enumModel.getOrigin(), name, enumModel.getKind(), enumModel.getMembers(), enumModel.getComments(), isNonConstEnum);
    }

    public EnumKind getKind() {
        return kind;
    }

    public List<EnumMemberModel> getMembers() {
        return members;
    }

    public boolean isNonConstEnum() {
        return isNonConstEnum;
    }

    public TsEnumModel withMembers(List<EnumMemberModel> members) {
        return new TsEnumModel(origin, name, kind, members, comments, isNonConstEnum);
    }

    public TsEnumModel withComments(List<String> comments) {
        return new TsEnumModel(origin, name, kind, members, comments, isNonConstEnum);
    }

}
