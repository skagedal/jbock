package net.jbock.coerce;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import net.jbock.coerce.mappers.AllCoercions;
import net.jbock.coerce.mappers.CoercionFactory;
import net.jbock.coerce.mappers.EnumCoercion;
import net.jbock.coerce.mappers.MapperCoercion;
import net.jbock.coerce.warn.WarningProvider;
import net.jbock.compiler.TypeTool;
import net.jbock.compiler.Util;
import net.jbock.compiler.ValidationException;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import static javax.lang.model.element.Modifier.FINAL;
import static net.jbock.coerce.CoercionKind.findKind;
import static net.jbock.coerce.mappers.MapperCoercion.mapperInit;
import static net.jbock.coerce.mappers.MapperCoercion.mapperMap;
import static net.jbock.compiler.Util.AS_DECLARED;
import static net.jbock.compiler.Util.QUALIFIED_NAME;

public class CoercionProvider {

  private static CoercionProvider instance;

  public static CoercionProvider getInstance() {
    if (instance == null) {
      instance = new CoercionProvider();
    }
    return instance;
  }

  public Coercion findCoercion(
      ExecutableElement sourceMethod,
      String paramName,
      TypeElement mapperClass,
      TypeElement collectorClass,
      boolean repeatable,
      boolean optional) {
    if (repeatable && optional) {
      throw ValidationException.create(sourceMethod,
          "The parameter can be repeatable or optional, but not both.");
    }
    if (collectorClass != null && !repeatable) {
      throw ValidationException.create(sourceMethod,
          "The parameter must be declared repeatable in order to have a collector.");
    }
    TypeMirror returnType = sourceMethod.getReturnType();
    try {
      return handle(sourceMethod, paramName, mapperClass, collectorClass, repeatable, optional);
    } catch (TmpException e) {
      if (!e.findWarning()) {
        throw e.asValidationException(sourceMethod);
      }
      String warning = WarningProvider.instance().findWarning(returnType, repeatable, optional);
      if (warning != null) {
        throw e.asValidationException(sourceMethod, warning);
      }
      throw e.asValidationException(sourceMethod);
    }
  }

  private Coercion handle(
      ExecutableElement sourceMethod,
      String paramName,
      TypeElement mapperClass,
      TypeElement collectorClass,
      boolean repeatable,
      boolean optional) throws TmpException {
    FieldSpec field = FieldSpec.builder(TypeName.get(sourceMethod.getReturnType()),
        snakeToCamel(paramName))
        .addModifiers(FINAL)
        .build();
    if (repeatable) {
      return handleRepeatable(sourceMethod, paramName, mapperClass, collectorClass, field);
    }
    if (mapperClass != null) {
      return handleMapper(sourceMethod, paramName, mapperClass, field, optional);
    }
    return handleSimple(sourceMethod, optional, field);
  }

  // no mapper or collector
  private Coercion handleSimple(
      ExecutableElement sourceMethod,
      boolean optional,
      FieldSpec field) throws TmpException {
    TypeMirror returnType = sourceMethod.getReturnType();
    if (returnType.getKind() == TypeKind.ARRAY) {
      // there's no default mapper for array
      throw TmpException.create("Either switch to List and declare this parameter repeatable, or use a custom mapper.");
    }
    TriggerKind tk = trigger(returnType, optional);
    return handleDefault(tk, field, optional);
  }

  private Coercion handleRepeatable(
      ExecutableElement sourceMethod,
      String paramName,
      TypeElement mapperClass,
      TypeElement collectorClass,
      FieldSpec field) throws TmpException {
    TypeMirror returnType = sourceMethod.getReturnType();
    CollectorInfo collectorInput = collectorInput(sourceMethod, collectorClass, returnType);
    TriggerKind tk = CoercionKind.SIMPLE.of(collectorInput.collectorInput, collectorInput);
    if (mapperClass == null) {
      CoercionFactory coercion = AllCoercions.get(collectorInput.collectorInput);
      if (coercion == null) {
        throw TmpException.findWarning(String.format("Unknown collector input %s, please define a custom mapper.", collectorInput.collectorInput));
      }
      return coercion.getCoercion(field, tk);
    }
    TypeMirror trigger = MapperClassValidator.findReturnType(mapperClass, collectorInput.collectorInput);
    collectorInput = collectorInput.withInput(trigger);
    tk = CoercionKind.SIMPLE.of(trigger, collectorInput);
    TypeName mapperType = TypeName.get(mapperClass.asType());
    ParameterSpec mapperParam = ParameterSpec.builder(mapperType, snakeToCamel(paramName) + "Mapper").build();
    return MapperCoercion.create(tk, mapperParam, mapperClass.asType(), field);
  }

  private Coercion handleMapper(
      ExecutableElement sourceMethod,
      String paramName,
      TypeElement mapperClass,
      FieldSpec field,
      boolean optional) throws TmpException {
    TypeName mapperType = TypeName.get(mapperClass.asType());
    ParameterSpec mapperParam = ParameterSpec.builder(mapperType, snakeToCamel(paramName) + "Mapper").build();
    TriggerKind tk = trigger(sourceMethod.getReturnType(), optional);
    MapperSkew skew = mapperSkew(tk);
    MapperClassValidator.findReturnType(mapperClass, skew != null ? skew.mapperReturnType : tk.trigger);
    if (skew != null) {
      CoercionFactory coercionFactory = AllCoercions.get(skew.baseType);
      return coercionFactory.getCoercion(field, tk)
          .withMapper(mapperMap(mapperParam), mapperInit(skew.mapperReturnType, mapperParam, mapperClass.asType()));
    } else {
      return MapperCoercion.create(tk, mapperParam, mapperClass.asType(), field);
    }
  }

  private MapperSkew mapperSkew(TriggerKind tk) {
    if (tk.kind == CoercionKind.OPTIONAL_COMBINATION) {
      return null;
    }
    if (!tk.collectorInfo.collectorInit.isEmpty()) {
      return null;
    }
    if (tk.trigger.getKind().isPrimitive()) {
      return MapperSkew.create(TypeTool.get().box(tk.trigger));
    }
    if (TypeTool.get().equals(tk.trigger, OptionalInt.class)) {
      return MapperSkew.create(TypeTool.get().declared(Integer.class), tk.trigger);
    } else if (TypeTool.get().equals(tk.trigger, OptionalDouble.class)) {
      return MapperSkew.create(TypeTool.get().declared(Double.class), tk.trigger);
    } else if (TypeTool.get().equals(tk.trigger, OptionalLong.class)) {
      return MapperSkew.create(TypeTool.get().declared(Long.class), tk.trigger);
    }
    return null;
  }

  private Coercion handleDefault(
      TriggerKind tk,
      FieldSpec field,
      boolean optional) throws TmpException {
    CoercionFactory enumCoercion = checkEnum(tk.trigger);
    if (enumCoercion != null) {
      return enumCoercion.getCoercion(field, tk);
    } else {
      CoercionFactory factory = AllCoercions.get(tk.trigger);
      if (factory == null) {
        throw TmpException.findWarning("Bad return type");
      }
      if (factory.handlesOptionalPrimitive() && !optional) {
        throw TmpException.findWarning("Declare this parameter optional.");
      }
      return factory.getCoercion(field, tk);
    }
  }

  private CoercionFactory checkEnum(TypeMirror mirror) throws TmpException {
    if (mirror.getKind() != TypeKind.DECLARED) {
      return null;
    }
    DeclaredType declared = mirror.accept(AS_DECLARED, null);
    TypeElement element = declared.asElement().accept(Util.AS_TYPE_ELEMENT, null);
    TypeMirror superclass = element.getSuperclass();
    if (!"java.lang.Enum".equals(superclass.accept(QUALIFIED_NAME, null))) {
      return null;
    }
    if (element.getModifiers().contains(Modifier.PRIVATE)) {
      throw TmpException.findWarning("Private return type is not allowed");
    }
    return EnumCoercion.create(mirror);
  }

  private TriggerKind trigger(
      TypeMirror returnType,
      boolean optional) {
    DeclaredType parameterized = Util.asParameterized(returnType);
    if (parameterized == null) {
      // not a combination, triggered by return type
      return CoercionKind.SIMPLE.of(returnType, CollectorInfo.empty());
    }
    CoercionKind kind = findKind(parameterized);
    if (optional && kind.isCombination()) {
      return kind.of(parameterized.getTypeArguments().get(0), CollectorInfo.empty());
    }
    return kind.of(parameterized, CollectorInfo.empty());
  }

  private CollectorInfo collectorInput(
      ExecutableElement sourceMethod,
      TypeElement collectorClass,
      TypeMirror returnType) throws TmpException {
    if (collectorClass == null) {
      TypeTool tool = TypeTool.get();
      if (!tool.equals(tool.erasure(returnType), tool.declared(List.class))) {
        throw TmpException.create("Either define a custom collector, or return List");
      }
      List<? extends TypeMirror> typeParameters = returnType.accept(TypeTool.TYPEARGS, null);
      if (typeParameters.isEmpty()) {
        throw TmpException.create("Either define a custom collector, or return List");
      }
      return CollectorInfo.listCollector(typeParameters.get(0));
    }
    return CollectorInfo.create(CollectorClassValidator.findInputType(sourceMethod.getReturnType(), collectorClass), collectorClass);
  }

  static String snakeToCamel(String s) {
    StringBuilder sb = new StringBuilder();
    boolean upcase = false;
    boolean underscore = false;
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c == '_') {
        if (underscore) {
          sb.append('_');
        }
        underscore = true;
        upcase = true;
      } else {
        underscore = false;
        if (upcase) {
          sb.append(Character.toUpperCase(c));
          upcase = false;
        } else {
          sb.append(Character.toLowerCase(c));
        }
      }
    }
    return sb.toString();
  }
}
