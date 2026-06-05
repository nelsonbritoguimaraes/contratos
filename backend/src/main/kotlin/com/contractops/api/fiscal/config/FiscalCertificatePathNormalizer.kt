package com.contractops.api.fiscal.config

import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Resolve caminhos relativos de certificado (ex.: ./config/certs/esocial.pfx)
 * a partir da raiz do repositório ou CONTRACTOPS_HOME.
 */
@Component
class FiscalCertificatePathNormalizer(
    private val fiscalProperties: FiscalProperties
) {
    @PostConstruct
    fun normalize() {
        val base = System.getenv("CONTRACTOPS_HOME")?.let { Paths.get(it) }
            ?: findRepositoryRoot()
            ?: Paths.get(System.getProperty("user.dir"))

        fiscalProperties.esocial.certificatePath =
            resolvePath(fiscalProperties.esocial.certificatePath, base)
        fiscalProperties.nfse.certificatePath =
            resolvePath(fiscalProperties.nfse.certificatePath, base)
    }

    private fun resolvePath(raw: String?, base: Path): String? {
        if (raw.isNullOrBlank()) return raw
        val path = Paths.get(raw.trim())
        if (path.isAbsolute) return path.normalize().toString()
        return base.resolve(path).normalize().toAbsolutePath().toString()
    }

    private fun findRepositoryRoot(): Path? {
        var current = Paths.get(System.getProperty("user.dir")).toAbsolutePath()
        repeat(6) {
            if (Files.isDirectory(current.resolve("config").resolve("certs"))) return current
            if (Files.exists(current.resolve("backend").resolve("build.gradle.kts"))) return current
            current = current.parent ?: return null
        }
        return null
    }
}
