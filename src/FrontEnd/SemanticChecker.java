package FrontEnd;

import java.util.ArrayList;
import java.util.Stack;
import AST.*;
import Util.scope.*;
import Util.symbol.*;
import Util.error.*;
import Util.position;

//principle: set new scope before visit its body;
//(so check condition: {...;{};...})
//principle: always visit a node before get its type;
//check type and variable principle
public class SemanticChecker implements ASTVisitor {

    private semanticError binaryCalError(Type typeO, Type typeT,
                                         AST.binaryExpr.opCategory opCode, position pos) {
        return new semanticError("Incorrect calculation, type Error, type is: '" +
                                 typeO.toString() + "' and '" + typeT.toString() +
                                 "', opCode is " + opCode.toString(), pos);
    }

    globalScope gScope;
    Scope currentScope;
    classType currentClass;
    Type currentRetType;
    Stack<Integer> loopStack;

    public SemanticChecker(globalScope gScope) {
        this.gScope = gScope;
        currentScope = gScope;
        currentClass = null;
        currentRetType = null;
        loopStack = new Stack<>();
    }

    @Override
    public void visit(rootNode it) {
        if (!it.allDef().isEmpty()) {
            it.allDef().forEach(node -> node.accept(this));
        }
    }


    @Override
    public void visit(classDef it) {
        classType defClass = (classType)gScope.getType(it.Identifier(), it.pos());
        currentScope = defClass.scope();
        currentClass = defClass;
        it.members().forEach(method -> method.accept(this));
        it.methods().forEach(method -> method.accept(this));
        it.constructors().forEach(constructor->constructor.accept(this));
        currentClass = null;
        currentScope = currentScope.parentScope();
    }

    @Override
    public void visit(funDef it) {
        if(it.isConstructor()) {
            currentRetType = gScope.getVoidType();
        } else currentRetType = currentScope.getMethod(it.Identifier(), it.pos()).returnType();

        //parameters are already in the scope(in TypeFilter)
        currentScope = currentScope.getMethod(it.Identifier(), it.pos()).scope();
        it.body().accept(this);
        currentScope = currentScope.parentScope();

        currentRetType = null;
    }

    @Override
    public void visit(varDef it) {
        currentScope.defineMember(it.name(),
                                  new varEntity(it.name(), gScope.generateType(it.type())),
                                  it.pos());
    }

    @Override   //not used
    public void visit(varDefList it) {}


    @Override
    public void visit(blockNode it) {
        it.getStmtList().forEach(stmt -> {
            if (stmt instanceof blockNode) {
                currentScope = new Scope(currentScope);
                stmt.accept(this);
                currentScope = currentScope.parentScope();
            } else stmt.accept(this);
        }); //cannot be null
    }


    @Override
    public void visit(exprStmt it) {
        it.expr().accept(this);
    }

    @Override
    public void visit(ifStmt it) {
        it.condition().accept(this);
        if (!it.condition().type().isBool())
            throw new semanticError("not a bool", it.condition().pos());

        currentScope = new Scope(currentScope);
        it.trueStmt().accept(this);
        currentScope = currentScope.parentScope();

        if (it.falseStmt() != null) {
            currentScope = new Scope(currentScope);
            it.falseStmt().accept(this);
            currentScope = currentScope.parentScope();
        }
    }

    @Override
    public void visit(forStmt it) {
        if (it.init() != null)
            it.init().accept(this);

        if (it.incr() != null)
            it.incr().accept(this);

        if (it.condition() != null)
            it.condition().accept(this);

        if (!it.condition().type().isBool())
            throw new semanticError("not a bool", it.condition().pos());

        currentScope = new Scope(currentScope);
        loopStack.push(0);
        it.body().accept(this);
        loopStack.pop();
        currentScope = currentScope.parentScope();
    }

    @Override
    public void visit(whileStmt it) {
        if (it.condition() != null)
            it.condition().accept(this);

        if (!it.condition().type().isBool())
            throw new semanticError("not a bool", it.condition().pos());

        currentScope = new Scope(currentScope);
        loopStack.push(0);
        it.body().accept(this);
        loopStack.pop();
        currentScope = currentScope.parentScope();
    }

    @Override
    public void visit(returnStmt it) {
        if (it.retValue() != null) {
            it.retValue().accept(this);
            if (currentRetType.sameType(it.retValue().type()))
                throw new semanticError("not the correct return type: is " +
                                        it.retValue().type().toString() + ", should be: " +
                                        currentRetType.toString(), it.pos());
        } else if (!currentRetType.isVoid()) {
            throw new semanticError("not the correct return type: is Void, should be: " +
                                    currentRetType.toString(), it.pos());
        }
    }

    @Override
    public void visit(breakStmt it) {
        if (loopStack.isEmpty())
            throw new semanticError("break not in loop", it.pos());
    }

    @Override
    public void visit(continueStmt it) {
        if (loopStack.isEmpty())
            throw new semanticError("continue not in loop", it.pos());
    }

    @Override
    public void visit(emptyStmt it) {}


    @Override   //more check in funcCall
    public void visit(exprList it) {
        it.params().forEach(param -> param.accept(this));   //cannot be null
    }

    //no need to visit it, only use it to form type;
    @Override
    public void visit(typeNode it) {}


    @Override
    public void visit(arrayExpr it) {
        it.base().accept(this);
        it.width().accept(this);
        it.setType(new arrayType(it.base().type()));
    }

    @Override
    public void visit(binaryExpr it){
        it.src1().accept(this);
        it.src2().accept(this);
        Type typeO, typeT;
        typeO = it.src1().type();
        typeT = it.src2().type();
        AST.binaryExpr.opCategory opCode = it.opCode();
        if (opCode.ordinal() < 10) {
            if (!(typeO.isInt() && typeT.isInt()))
                throw binaryCalError(typeO, typeT, opCode, it.pos());
            it.setType(gScope.getIntType());
        } else if (opCode.ordinal() < 14) {
            if (!( (typeO.isInt() || typeO.sameType(gScope.getStringType()))
                    && (typeO.sameType(typeT)) ))
                throw binaryCalError(typeO, typeT, opCode, it.pos());
            it.setType(typeO);
        } else if (opCode.ordinal() < 16) {
            if (!(typeO.isInt() && typeT.isInt()))
                throw binaryCalError(typeO, typeT, opCode, it.pos());
            it.setType(gScope.getBoolType());
        } else if (opCode.ordinal() < 18) {
            if (!typeO.sameType(typeT))
                throw binaryCalError(typeO, typeT, opCode, it.pos());
            it.setType(gScope.getBoolType());
        } else {
            if (!typeO.sameType(typeT))
                throw binaryCalError(typeO, typeT, opCode, it.pos());
            it.setType(typeO);
        }
    }

    @Override
    public void visit(prefixExpr it) {
        it.src().accept(this);
        if (!it.src().type().isInt())
            throw new semanticError("operator not match. Type: " +
                                    it.src().type().toString(), it.pos());
    }

    @Override
    public void visit(suffixExpr it) {
        it.src().accept(this);
        if (!it.src().type().isInt())
            throw new semanticError("operator not match. Type: " +
                    it.src().type().toString(), it.pos());
    }


    @Override
    public void visit(thisExpr it) {
        if (currentClass == null)
            throw new semanticError("this expr not in a class", it.pos());
        it.setPointClass(currentClass);
        it.setType(currentClass);
    }

    //the type of the constructor function funcCall should be the type of the class(help newExpr get type)
    @Override
    public void visit(funCallExpr it) {
        it.callee().accept(this);
        if (it.callee().type() instanceof funcDecl) {
            funcDecl func = (funcDecl)it.callee().type();
            ArrayList<varEntity> args = func.scope().params();
            ArrayList<exprNode> params = it.params();
            params.forEach(param -> param.accept(this));   //cannot be null
            if (params.size() != args.size())
                throw new semanticError("", it.pos());
            for (int i = 0; i < args.size(); i++) {
                if (!(args.get(i).type().sameType(params.get(i).type())) )
                    throw new semanticError("parameter type not match. is: '" +
                                            params.get(i).type().toString() + "', should be :'" +
                                            args.get(i).type().toString() + "'", params.get(i).pos());
            }
        } else throw new semanticError("function not defined(as a function)", it.callee().pos());
    }

    @Override
    public void visit(memberExpr it) {
        it.caller().accept(this);
        if (!it.caller().type().isClass())
            throw new semanticError("not a class", it.caller().pos());
        classType callerClass = (classType)it.caller().type();
        if (callerClass.scope().containsMember(it.member())) {
            it.setType(callerClass.scope().getMemberType(it.member(), it.pos()));
        } else if (callerClass.scope().containsMethod(it.member())){
            it.setType(callerClass.scope().getMethod(it.member(), it.pos()));
        }
    }

    @Override
    public void visit(newExpr it) {
        it.exprs().forEach(expr -> {
            expr.accept(this);
            if (expr.type().isInt())
                throw new semanticError("not a int", expr.pos());
        }); //cannot be null
        it.setType(gScope.generateType(it.typeN()));
    }


    @Override
    public void visit(varNode it){
        it.setType(currentScope.getMemberType(it.name(), it.pos()));
    }

    @Override
    public void visit(intLiteral it) {
        it.setType(gScope.getIntType());
    }

    @Override
    public void visit(boolLiteral it) {
        it.setType(gScope.getBoolType());
    }

    @Override
    public void visit(nullLiteral it){
        it.setType(gScope.getNullType());
    }

    @Override
    public void visit(stringLiteral it){
        it.setType(gScope.getType("string", it.pos()));
    }
}
