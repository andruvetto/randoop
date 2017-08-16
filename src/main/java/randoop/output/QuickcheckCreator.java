package randoop.output;

import static randoop.output.NameGenerator.numDigits;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.TokenMgrError;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.ReferenceType;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import randoop.main.GenInputsAbstract;
import randoop.sequence.ExecutableSequence;

public class QuickcheckCreator {

  private final String packageName;
  /**
   * classMethodCounts maps quickcheck class names to the number of methods in each class. This is
   * used to generate lists of method names for a class, since current convention is that a test
   * method is named "test"+i for some integer i.
   */
  private Map<String, Integer> classMethodCounts;

  public QuickcheckCreator(String packageName) {
    this.packageName = packageName;
    this.classMethodCounts = new LinkedHashMap<>();
  }

  public CompilationUnit createQuickcheckClass(
      String className, String methodPrefix, List<ExecutableSequence> sequences) {

    this.classMethodCounts.put(className, sequences.size());

    CompilationUnit compilationUnit = new CompilationUnit();
    if (packageName != null && !packageName.isEmpty()) {
      compilationUnit.setPackage(new PackageDeclaration(new NameExpr(packageName)));
    }

    // class declaration
    ClassOrInterfaceDeclaration classDeclaration =
        new ClassOrInterfaceDeclaration(Modifier.PUBLIC | Modifier.FINAL, false, className);

    List<BodyDeclaration> bodyDeclarations = new ArrayList<>();

    NameGenerator methodNameGen = new NameGenerator(methodPrefix, 1, numDigits(sequences.size()));
    for (ExecutableSequence s : sequences) {
      MethodDeclaration method = createMethod(className, methodNameGen.next(), s);
      if (method != null) {
        bodyDeclarations.add(method);
      }
    }
    classDeclaration.setMembers(bodyDeclarations);
    List<TypeDeclaration> types = new ArrayList<>();
    types.add(classDeclaration);
    compilationUnit.setTypes(types);
    return compilationUnit;
  }

  private MethodDeclaration createMethod(
      String className, String methodName, ExecutableSequence sequence) {

    String classTypeName = GenInputsAbstract.testclass.get(0);
    String returnVariable = "";
    String statementOutputType = "";
    String sequenceBlockString = "{ ";
    ClassOrInterfaceType classType = new ClassOrInterfaceType();

    int lastIndex = sequence.sequence.size() - 1;
    int nonNormalExecutionIndex = sequence.getNonNormalExecutionIndex();
    if (nonNormalExecutionIndex > -1) {
      lastIndex = nonNormalExecutionIndex;
    }

    for (int i = 0; i < lastIndex; i++) {
      sequenceBlockString += sequence.statementToCodeString(i);
      statementOutputType = sequence.sequence.statements.get(i).getOutputType().getCanonicalName();
      if (statementOutputType.equals(classTypeName)) {
        returnVariable = sequence.sequence.getVariable(i).getName();
        classType = new ClassOrInterfaceType(sequence.sequence.getVariable(i).getType().getName());
      }
    }

    sequenceBlockString += "return " + returnVariable + "; \n }";

    if (returnVariable.isEmpty()) {
      return null;
    }

    MethodDeclaration method =
        new MethodDeclaration(Modifier.PUBLIC | Modifier.STATIC, classType, methodName);

    List<ReferenceType> throwsList = new ArrayList<>();
    throwsList.add(new ReferenceType(new ClassOrInterfaceType("Throwable")));
    method.setThrows(throwsList);

    BlockStmt body = new BlockStmt();
    List<Statement> statements = new ArrayList<>();
    FieldAccessExpr field = new FieldAccessExpr(new NameExpr("System"), "out");
    MethodCallExpr call = new MethodCallExpr(field, "format");

    List<Expression> arguments = new ArrayList<>();
    arguments.add(new StringLiteralExpr("%n%s%n"));
    arguments.add(new StringLiteralExpr(className + "." + methodName));
    call.setArgs(arguments);

    try {
      BlockStmt sequenceBlock = JavaParser.parseBlock(sequenceBlockString);
      statements.addAll(sequenceBlock.getStmts());
    } catch (ParseException e) {
      System.out.println(
          "Parse error while creating method " + className + "." + methodName + " for block ");
      System.out.println(sequenceBlockString);
      return null;
    } catch (TokenMgrError e) {
      System.out.println("Lexical error while creating method " + className + "." + methodName);
      System.out.println("Exception: " + e.getMessage());
      System.out.println(sequenceBlockString);
      return null;
    }
    body.setStmts(statements);
    method.setBody(body);
    return method;
  }

  /** @return the packageName */
  public String getPackageName() {
    return packageName;
  }
}
