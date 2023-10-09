package patch

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Path
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import kotlin.io.path.deleteIfExists
import kotlin.io.path.isRegularFile

interface Patch {
    val classFile: String

    fun transform(node: ClassNode)
}

fun patchJar(inputFile: Path, outputFile: Path, patches: List<Patch>) {
    if (!inputFile.isRegularFile()) {
        throw IOException("Provided path $inputFile is not a file!")
    }

    outputFile.deleteIfExists()

    val inJar = JarFile(inputFile.toFile())
    val patchesByClass: Map<String, List<Patch>> = patches.map { it.classFile }.distinct()
        .associateWith { classFile -> patches.filter { it.classFile == classFile } }

    JarOutputStream(FileOutputStream(outputFile.toFile())).use { jarOutputStream ->
        for (entry in inJar.entries()) {
            val bytes = inJar.getInputStream(entry).readAllBytes()

            val patchList = patchesByClass[entry.name]

            if (patchList.isNullOrEmpty()) {
                jarOutputStream.putNextEntry(entry)
                jarOutputStream.write(bytes)
                jarOutputStream.closeEntry()
                continue
            }

            val reader = ClassReader(bytes)
            val node = ClassNode()
            reader.accept(node, ClassReader.EXPAND_FRAMES)

            for (patch in patchList) {
                patch.transform(node)
            }

            // Note: Passing 0 here is dangerous if we have patches that actually
            // change how much of the stack or how many locals are used.
            val writer = ClassWriter(0)
            node.accept(writer)

            jarOutputStream.putNextEntry(entry)
            jarOutputStream.write(writer.toByteArray())
            jarOutputStream.closeEntry()
        }
    }
}
