package org.echoosx.mirai.plugin.util

/**
 * @Author Niuxy
 * @Date 2020/9/30 1:12 下午
 * @Description 字符串模糊匹配
 */
object StringCompareUtil {
    /**
     * @Author Niuxy
     * @Date 2020/9/30 2:08 下午
     * @Description str1, str2 待比较字符串，threshold: 比较阈值，redundances: 冗余信息项
     */
    fun isSame(str1: String?, str2: String?, threshold: Double, redundances: Array<String>): Boolean {
        var str1 = str1
        var str2 = str2
        if (str1 == null || str2 == null || str1.isEmpty() || str2.isEmpty()) throw NullPointerException("str1 or str2 is null")
        str1 = deleteRedundances(str1, redundances)
        str2 = deleteRedundances(str2, redundances)
        val length = Math.min(str1.length, str2.length)
        return isSame(str1, str2, length, threshold)
    }

    //比较重合率与阈值
    private fun isSame(str1: String?, str2: String?, length: Int, threshold: Double): Boolean {
        val re = coincidenceRate(str1, str2, length)
        return re >= threshold
    }

    //计算重合率
    fun coincidenceRate(str1: String?, str2: String?, length: Int): Double {
        val coincidence = longestCommonSubsequence(str1, str2)
        return txfloat(coincidence, length)
    }

    //去处冗余
    private fun deleteRedundances(str: String, redundances: Array<String>): String {
        val stringBuilder = StringBuilder(str)
        for (redundance in redundances) {
            val index = stringBuilder.indexOf(redundance)
            if (index != -1) stringBuilder.replace(index, index + redundance.length, "")
        }
        return stringBuilder.toString()
    }

    //计算最长公共子序列
    private fun longestCommonSubsequence(str1: String?, str2: String?): Int {
        if (str1 == null || str2 == null) return 0
        val m = str1.length
        val n = str2.length
        val cache = Array(m + 1) { IntArray(n + 1) }
        for (i in m - 1 downTo 0) {
            for (j in n - 1 downTo 0) {
                if (str1[i] == str2[j]) cache[i][j] = cache[i + 1][j + 1] + 1 else cache[i][j] = Math.max(
                    cache[i][j + 1], cache[i + 1][j])
            }
        }
        return cache[0][0]
    }

    //相除取两位小数
    private fun txfloat(a:Int, b:Int): Double {
        return (a.toFloat() / b).toDouble()
    }
}