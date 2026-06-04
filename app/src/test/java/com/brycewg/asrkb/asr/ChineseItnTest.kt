// 中文规则 ITN 的 JVM 回归测试。
package com.brycewg.asrkb.asr

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class ChineseItnTest {
    @Test
    fun parsesTensRangeExpression() {
        assertEquals("34~35", ChineseItnRanges.parseRange("三十四五"))
    }

    @Test
    fun normalizesChineseItnReferenceCases() {
        val cases = loadChineseItnReferenceCases()
        val failures = cases.mapNotNull { (input, expected) ->
            val actual = ChineseItn.normalize(input)
            if (actual == expected) null else "input=[$input]\nexpected=[$expected]\nactual=[$actual]"
        }

        if (failures.isNotEmpty()) {
            fail("Chinese-ITN reference mismatches: ${failures.size}/${cases.size}\n" + failures.take(80).joinToString("\n\n"))
        }
    }

    @Test
    fun preservesProtectedExpressions() {
        val cases = listOf(
            "不管三七二十一",
            "九九八十一难",
            "一点一滴",
            "仅一个多月的时间里",
            "一共有多少人",
            "十三五规划期间获得了十几万和几十万甚至二十几万的投资",
            "他是一个千万富翁",
            "你可千万别这样",
            "我要去看看这万千世界",
            "这里有一点地方不是太对"
        )

        cases.forEach { input ->
            assertEquals(input, input, ChineseItn.normalize(input))
        }
    }

    private fun loadChineseItnReferenceCases(): List<Pair<String, String>> {
        val file = findRepoFile("references/Chinese-ITN/test_cases.txt")
        return file.readLines(Charsets.UTF_8)
            .asSequence()
            .map { it.trimEnd() }
            .filter { it.isNotBlank() && !it.trimStart().startsWith("#") }
            .mapNotNull { line ->
                val separator = " -> "
                val index = line.indexOf(separator)
                if (index < 0) return@mapNotNull null
                line.substring(0, index) to line.substring(index + separator.length).trimEnd()
            }
            .toList()
    }

    private fun findRepoFile(relativePath: String): File {
        var dir = File(System.getProperty("user.dir")).absoluteFile
        while (true) {
            val candidate = File(dir, relativePath)
            if (candidate.exists()) return candidate
            val parent = dir.parentFile ?: error("Cannot find $relativePath from ${System.getProperty("user.dir")}")
            dir = parent
        }
    }
}
