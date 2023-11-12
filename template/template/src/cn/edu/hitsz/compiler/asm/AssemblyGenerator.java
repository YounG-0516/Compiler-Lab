package cn.edu.hitsz.compiler.asm;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.ir.*;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;


/**
 * TODO: 实验四: 实现汇编生成
 * <br>
 * 在编译器的整体框架中, 代码生成可以称作后端, 而前面的所有工作都可称为前端.
 * <br>
 * 在前端完成的所有工作中, 都是与目标平台无关的, 而后端的工作为将前端生成的目标平台无关信息
 * 根据目标平台生成汇编代码. 前后端的分离有利于实现编译器面向不同平台生成汇编代码. 由于前后
 * 端分离的原因, 有可能前端生成的中间代码并不符合目标平台的汇编代码特点. 具体到本项目你可以
 * 尝试加入一个方法将中间代码调整为更接近 risc-v 汇编的形式, 这样会有利于汇编代码的生成.
 * <br>
 * 为保证实现上的自由, 框架中并未对后端提供基建, 在具体实现时可自行设计相关数据结构.
 *
 * @see AssemblyGenerator#run() 代码生成与寄存器分配
 */
public class AssemblyGenerator {

    /**
     * 存放经过预处理后的中间指令
     */
    private final List<Instruction> instructions = new ArrayList<>();

    /**
     * 变量与寄存器双向map
     */
    BMap<IRValue, Register> registerMap = new BMap<>();

    /**
     * 生成的汇编指令列表
     */
    private final List<String> asmInstructions = new ArrayList<>(List.of(".text"));


    /**
     * 寄存器
     */
    enum Register {
        t0,t1,t2,t3,t4,t5,t6
    }

    /**
     * 加载前端提供的中间代码
     * <br>
     * 视具体实现而定, 在加载中或加载后会生成一些在代码生成中会用到的信息. 如变量的引用
     * 信息. 这些信息可以通过简单的映射维护, 或者自行增加记录信息的数据结构.
     *
     * @param originInstructions 前端提供的中间代码
     */
    public void loadIR(List<Instruction> originInstructions) {
        // TODO: 读入前端提供的中间代码并生成所需要的信息
        for (Instruction instruction : originInstructions){
            InstructionKind instructionKind = instruction.getKind();
            // 如果是RET指令
            if (instructionKind.isReturn()){
                // 遇到Ret指令后直接舍弃后续指令
                instructions.add(instruction);
                break;
            }
            //如果是一个操作数的指令
            if(instructionKind.isUnary()){
                instructions.add(instruction);
            }
            //如果是两个操作数的指令
            else if(instructionKind.isBinary()){
                IRValue lhs = instruction.getLHS();
                IRValue rhs = instruction.getRHS();
                IRVariable result = instruction.getResult();
                // 如果两个操作数都是立即数：将操作两个立即数的BinaryOp直接进行求值得到结果，然后替换成MOV指令
                if (lhs.isImmediate() && rhs.isImmediate()){
                    int immediateResult = 0;
                    switch (instructionKind){
                        case ADD -> immediateResult = ((IRImmediate)lhs).getValue() + ((IRImmediate)rhs).getValue();
                        case SUB -> immediateResult = ((IRImmediate)lhs).getValue() - ((IRImmediate)rhs).getValue();
                        case MUL -> immediateResult = ((IRImmediate)lhs).getValue() * ((IRImmediate)rhs).getValue();
                        default -> System.out.println("error");
                    }
                    instructions.add(Instruction.createMov(result, IRImmediate.of(immediateResult)));
                }
                // 左立即数修改指令
                else if (lhs.isImmediate() && rhs.isIRVariable()){
                    switch (instructionKind){
                        // 加法交换左立即数至右边
                        case ADD -> instructions.add(Instruction.createAdd(result, rhs, lhs));
                        // 减法与乘法指令前添加 MOV temp imm
                        case SUB -> {
                            IRVariable temp = IRVariable.temp();
                            instructions.add(Instruction.createMov(temp, lhs));
                            instructions.add(Instruction.createSub(result, temp, rhs));
                        }
                        case MUL -> {
                            IRVariable temp = IRVariable.temp();
                            instructions.add(Instruction.createMov(temp, lhs));
                            instructions.add(Instruction.createMul(result, temp, rhs));
                        }
                        default -> System.out.println("error");
                    }
                }
                // 右立即数修改指令
                else if (lhs.isIRVariable() && rhs.isImmediate()){
                    switch (instructionKind){
                        case ADD, SUB -> instructions.add(instruction);
                        case MUL -> {
                            IRVariable temp = IRVariable.temp();
                            instructions.add(Instruction.createMov(temp, rhs));
                            instructions.add(Instruction.createMul(result, lhs, temp));
                        }
                        default -> System.out.println("error");
                    }
                }
                // 两个操作数均不为立即数，无需处理
                else {
                    instructions.add(instruction);
                }
            }
        }
    }

    /**
     * 分配寄存器
     */
    public void VariableToRegister(IRValue operands, int index){
        // 立即数无需分配寄存器
        if(operands.isImmediate()){
            return;
        }
        // 当前变量已经存在寄存器中，则无需再分配寄存器
        if(registerMap.containsKey(operands)){
            return;
        }
        // 寻找空闲寄存器
        for(Register register: Register.values()){
            if(!registerMap.containsValue(register)){
                registerMap.replace(operands, register);
                return;
            }
        }
        // 若无空闲寄存器，则夺取不再使用的变量所占的寄存器
        Set<Register> notUseRegs = Arrays.stream(Register.values()).collect(Collectors.toSet());
        for(int i = index; i<instructions.size(); i++){
            Instruction instruction = instructions.get(i);
            // 遍历搜寻不再使用的变量
            for(IRValue irValue: instruction.getAllOperands()){
                notUseRegs.remove(registerMap.getByKey(irValue));
            }
        }
        if(!notUseRegs.isEmpty()){
            registerMap.replace(operands, notUseRegs.iterator().next());
            return;
        }

        // 否则将无法分配寄存器并报错
        throw new RuntimeException("No enough registers!");
    }

    /**
     * 执行代码生成.
     * <br>
     * 根据理论课的做法, 在代码生成时同时完成寄存器分配的工作. 若你觉得这样的做法不好,
     * 也可以将寄存器分配和代码生成分开进行.
     * <br>
     * 提示: 寄存器分配中需要的信息较多, 关于全局的与代码生成过程无关的信息建议在代码生
     * 成前完成建立, 与代码生成的过程相关的信息可自行设计数据结构进行记录并动态维护.
     */
    public void run() {
        // TODO: 执行寄存器分配与代码生成
        int i=0;
        String asmCode = null;
        for(Instruction instruction: instructions){
            InstructionKind instructionKind = instruction.getKind();
            switch (instructionKind){
                case ADD -> {
                    IRValue lhs = instruction.getLHS();
                    IRValue rhs = instruction.getRHS();
                    IRVariable result = instruction.getResult();
                    VariableToRegister(lhs, i);
                    VariableToRegister(rhs, i);
                    VariableToRegister(result, i);
                    Register lhsReg = registerMap.getByKey(lhs);
                    Register rhsReg = registerMap.getByKey(rhs);
                    Register resultReg = registerMap.getByKey(result);
                    if(rhs.isImmediate()){
                        asmCode = String.format("\taddi %s, %s, %s", resultReg.toString(), lhsReg.toString(), rhs.toString());
                    }else{
                        asmCode = String.format("\tadd %s, %s, %s", resultReg.toString(), lhsReg.toString(), rhsReg.toString());
                    }
                }

                case SUB -> {
                    IRValue lhs = instruction.getLHS();
                    IRValue rhs = instruction.getRHS();
                    IRVariable result = instruction.getResult();
                    VariableToRegister(lhs, i);
                    VariableToRegister(rhs, i);
                    VariableToRegister(result, i);
                    Register lhsReg = registerMap.getByKey(lhs);
                    Register rhsReg = registerMap.getByKey(rhs);
                    Register resultReg = registerMap.getByKey(result);
                    if(rhs.isImmediate()){
                        asmCode = String.format("\tsubi %s, %s, %s", resultReg.toString(), lhsReg.toString(), rhs.toString());
                    }else{
                        asmCode = String.format("\tsub %s, %s, %s",resultReg.toString(), lhsReg.toString(), rhsReg.toString());
                    }
                }

                case MUL -> {
                    IRValue lhs = instruction.getLHS();
                    IRValue rhs = instruction.getRHS();
                    IRVariable result = instruction.getResult();
                    VariableToRegister(lhs, i);
                    VariableToRegister(rhs, i);
                    VariableToRegister(result, i);
                    Register lhsReg = registerMap.getByKey(lhs);
                    Register rhsReg = registerMap.getByKey(rhs);
                    Register resultReg = registerMap.getByKey(result);
                    asmCode = String.format("\tmul %s, %s, %s", resultReg.toString(), lhsReg.toString(), rhsReg.toString());
                }

                case MOV -> {
                    IRValue form = instruction.getFrom();
                    IRVariable result = instruction.getResult();
                    VariableToRegister(form, i);
                    VariableToRegister(result, i);
                    Register formReg = registerMap.getByKey(form);
                    Register resultReg = registerMap.getByKey(result);
                    if(form.isImmediate()){
                        asmCode = String.format("\tli %s, %s", resultReg.toString(), form.toString());
                    }else {
                        asmCode = String.format("\tmv %s, %s", resultReg.toString(), formReg.toString());
                    }
                }

                case RET -> {
                    IRValue returnValue = instruction.getReturnValue();
                    Register returnValueReg = registerMap.getByKey(returnValue);
                    asmCode = String.format("\tmv a0, %s", returnValueReg.toString());
                }

                default -> System.out.println("error asm!");
            }
            asmCode += "\t\t#  %s".formatted(instruction.toString());
            asmInstructions.add(asmCode);
            i++;

            if(instructionKind == InstructionKind.RET){
                break;
            }
        }
    }


    /**
     * 输出汇编代码到文件
     *
     * @param path 输出文件路径
     */
    public void dump(String path) {
        // TODO: 输出汇编代码到文件
        FileUtils.writeLines(path, asmInstructions);
    }
}

