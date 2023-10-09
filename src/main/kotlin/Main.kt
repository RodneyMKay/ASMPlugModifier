import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.IntInsnNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.TypeInsnNode
import patch.AddSimpleEnumValue
import patch.Patch
import patch.patchJar
import kotlin.io.path.*

fun main() {
    val inputFile = Path("input.jar")
    val outputFile = Path("output.jar")

    val patches = listOf(
        AddSimpleEnumValue(
            newValueName = "CUSTOM6",
            enumClass = "me/arcaniax/hdb/enums/CategoryEnum"
        ),
        AddHdbAddCategoryCall(
            categoryName = "CUSTOM6"
        ),
        AddSimpleEnumValue(
            newValueName = "CUSTOM7",
            enumClass = "me/arcaniax/hdb/enums/CategoryEnum"
        ),
        AddHdbAddCategoryCall(
            categoryName = "CUSTOM7"
        ),
        AddSimpleEnumValue(
            newValueName = "CUSTOM8",
            enumClass = "me/arcaniax/hdb/enums/CategoryEnum"
        ),
        AddHdbAddCategoryCall(
            categoryName = "CUSTOM8"
        ),
        AddSimpleEnumValue(
            newValueName = "CUSTOM9",
            enumClass = "me/arcaniax/hdb/enums/CategoryEnum"
        ),
        AddHdbAddCategoryCall(
            categoryName = "CUSTOM9"
        ),
    )

    patchJar(inputFile, outputFile, patches)
}

class AddHdbAddCategoryCall(
    private val categoryName: String
) : Patch {
    override val classFile = "me/arcaniax/hdb/HeadDatabaseManager.class"

    override fun transform(node: ClassNode) {
        val method = node.methods.find { it.name == "<init>" }!!
        val instructions = method.instructions
        val lastAddCategory = instructions.findLast {
            it.opcode == Opcodes.INVOKESPECIAL && it is MethodInsnNode && it.name == "addCategory"
        }

        val instructionList = InsnList().apply {
            add(IntInsnNode(Opcodes.ALOAD, 0))
            add(TypeInsnNode(Opcodes.NEW, "me/arcaniax/hdb/object/Category"))
            add(InsnNode(Opcodes.DUP))
            add(FieldInsnNode(Opcodes.GETSTATIC, "me/arcaniax/hdb/enums/CategoryEnum", categoryName, "Lme/arcaniax/hdb/enum/CategoryEnum;"))
            add(FieldInsnNode(Opcodes.GETSTATIC, "me/arcaniax/hdb/Main", "lang", "Lme/arcaniax/hdb/lang/Locale;"))
            add(LdcInsnNode("category.${categoryName.lowercase()}"))
            add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, "me/arcaniax/hdb/lang/Locale", "getText", "(Ljava/lang/String;)Ljava/lang/String;"))
            add(FieldInsnNode(Opcodes.GETSTATIC, "me/arcaniax/hdb/HeadDatabaseManager", "b64Custom", "Ljava/lang/String;"))
            add(InsnNode(Opcodes.ICONST_1))
            add(LdcInsnNode(""))
            add(LdcInsnNode(""))
            add(LdcInsnNode(""))
            add(MethodInsnNode(Opcodes.INVOKESTATIC, "java/util/UUID", "randomUUID", "()Ljava/util/UUID;"))
            add(MethodInsnNode(Opcodes.INVOKESTATIC, "me/arcaniax/hdb/util/HeadUtil", "create", "(Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/util/UUID;)Lorg/bukkit/inventory/ItemStack;"))
            add(MethodInsnNode(Opcodes.INVOKESPECIAL, "me/arcaniax/hdb/object/Category", "<init>", "(Lme/arcaniax/hdb/enums/CategoryEnum;Ljava/lang/String;Lorg/bukkit/inventory/ItemStack;)V"))
            add(MethodInsnNode(Opcodes.INVOKESPECIAL, "me/arcaniax/hdb/HeadDatabaseManager", "addCategory", "(Lme/arcaniax/hdb/object/Category;)V"))
        }

        instructions.insert(lastAddCategory, instructionList)
    }
}
