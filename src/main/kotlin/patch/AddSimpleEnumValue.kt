package patch

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.IntInsnNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.LineNumberNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.TypeInsnNode

class AddSimpleEnumValue(
    private val newValueName: String,
    private val enumClass: String
) : Patch {
    override val classFile = "$enumClass.class"

    override fun transform(node: ClassNode) {
        check(node.access and Opcodes.ACC_ENUM != 0)

        // Add new field
        val newNode = FieldNode(ENUM_FIELD_ACCESS, newValueName, "L$enumClass;", null, null)
        node.fields.add(newNode)

        // Change values method
        val valuesMethod = node.methods.find { it.name == "\$values" }
        checkNotNull(valuesMethod)
        val numberOfConstants = transformValuesMethod(valuesMethod)

        // Change class initialization
        val clinitMethod = node.methods.find { it.name == "<clinit>" }
        checkNotNull(clinitMethod)
        transformClassInitialization(clinitMethod, numberOfConstants)
    }

    private fun transformValuesMethod(node: MethodNode): Int {
        val instructions = node.instructions

        // Validate starting section
        check(instructions[0] is LabelNode)
        check(instructions[1] is LineNumberNode)

        val aNewArray = instructions[2] as IntInsnNode
        val numberOfConstants = aNewArray.operand
        aNewArray.operand++

        check(instructions[3].opcode == Opcodes.ANEWARRAY)

        // Validate DUP, BIPUSH, GETSTATIC, AASTORE repetition
        var currentIndex = 4

        repeat(numberOfConstants) {
            check(instructions[currentIndex].opcode == Opcodes.DUP)
            check(instructions[currentIndex + 1].isIConstOrBIPush())
            check(instructions[currentIndex + 2].opcode == Opcodes.GETSTATIC)
            check(instructions[currentIndex + 3].opcode == Opcodes.AASTORE)

            currentIndex += 4
        }

        // Insert new constant at the end of the repetition
        val instructionList = InsnList().apply { add(InsnNode(Opcodes.DUP))
            add(IntInsnNode(Opcodes.BIPUSH, numberOfConstants))
            add(FieldInsnNode(Opcodes.GETSTATIC, enumClass, newValueName, "L$enumClass;"))
            add(InsnNode(Opcodes.AASTORE))
        }

        instructions.insertBefore(instructions[currentIndex], instructionList)

        return numberOfConstants
    }

    private fun transformClassInitialization(node: MethodNode, numberOfConstants: Int) {
        val instructions = node.instructions

        // Validate beginning
        check(instructions[0] is LabelNode)
        check(instructions[1] is LineNumberNode)

        // Validate NEW, DUP, LDC, BIPUSH, INVOKESPECIAL, PUTSTATIC repetition
        var currentIndex = 2

        repeat(numberOfConstants) {
            check(instructions[currentIndex].opcode == Opcodes.NEW)
            check(instructions[currentIndex + 1].opcode == Opcodes.DUP)
            check(instructions[currentIndex + 2].opcode == Opcodes.LDC)
            check(instructions[currentIndex + 3].isIConstOrBIPush())
            check(instructions[currentIndex + 4].opcode == Opcodes.INVOKESPECIAL)
            check(instructions[currentIndex + 5].opcode == Opcodes.PUTSTATIC)

            currentIndex += 6
        }

        // Insert new instructions at the end of the repetition
        val instructionList = InsnList().apply {
            add(TypeInsnNode(Opcodes.NEW, enumClass))
            add(InsnNode(Opcodes.DUP))
            add(LdcInsnNode(newValueName))
            add(IntInsnNode(Opcodes.BIPUSH, numberOfConstants))
            add(MethodInsnNode(Opcodes.INVOKESPECIAL, enumClass, "<init>", "(Ljava/lang/String;I)V"))
            add(FieldInsnNode(Opcodes.PUTSTATIC, enumClass, newValueName, "L$enumClass;"))
        }

        instructions.insertBefore(instructions[currentIndex], instructionList)
    }

    companion object {
        private const val ENUM_FIELD_ACCESS = Opcodes.ACC_ENUM or
                Opcodes.ACC_STATIC or
                Opcodes.ACC_FINAL or
                Opcodes.ACC_PUBLIC
    }
}

private fun AbstractInsnNode.isIConstOrBIPush(): Boolean {
    return opcode == Opcodes.ICONST_0 ||
            opcode == Opcodes.ICONST_1 ||
            opcode == Opcodes.ICONST_2 ||
            opcode == Opcodes.ICONST_3 ||
            opcode == Opcodes.ICONST_4 ||
            opcode == Opcodes.ICONST_5 ||
            opcode == Opcodes.BIPUSH
}
