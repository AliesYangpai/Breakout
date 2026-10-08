package com.example.breakout.core

data class LevelConfig(
    val ballSpeed: Float,
    val paddleHalfWidth: Float,
    val rows: List<List<BrickType?>>
)

object Levels {

    // 行字符串：D=普通砖  2/3=耐久砖  I=硬砖  其他字符=空
    private fun rows(vararg lines: String): List<List<BrickType?>> = lines.map { line ->
        line.take(Field.COLS).map { c ->
            when (c) {
                'D' -> BrickType.NORMAL
                '2' -> BrickType.DURABLE_2
                '3' -> BrickType.DURABLE_3
                'I' -> BrickType.INDESTRUCTIBLE
                else -> null
            }
        }
    }

    val all: List<LevelConfig> = listOf(
        // 第 1 关：入门，3 排普通砖，慢球，宽挡板
        LevelConfig(320f, 70f, rows(
            "DDDDDDDDDD",
            "DDDDDDDDDD",
            "DDDDDDDDDD",
        )),
        // 第 2 关：4 排普通砖
        LevelConfig(340f, 70f, rows(
            "DDDDDDDDDD",
            "DDDDDDDDDD",
            "DDDDDDDDDD",
            "DDDDDDDDDD",
        )),
        // 第 3 关：5 排普通砖，标准挡板
        LevelConfig(380f, 55f, rows(
            "DDDDDDDDDD",
            "DDDDDDDDDD",
            "DDDDDDDDDD",
            "DDDDDDDDDD",
            "DDDDDDDDDD",
        )),
        // 第 4 关：引入耐久砖(2击)
        LevelConfig(400f, 55f, rows(
            "DDDDDDDDDD",
            "2222222222",
            "DDDDDDDDDD",
            "2222222222",
            "DDDDDDDDDD",
        )),
        // 第 5 关：耐久砖比例上升
        LevelConfig(420f, 55f, rows(
            "DDDDDDDDDD",
            "2222222222",
            "2222222222",
            "DDDDDDDDDD",
            "2222222222",
            "DDDDDDDDDD",
        )),
        // 第 6 关：高密度 + 3 击耐久砖
        LevelConfig(460f, 55f, rows(
            "2222222222",
            "DDDDDDDDDD",
            "2222222222",
            "3333333333",
            "DDDDDDDDDD",
            "2222222222",
        )),
        // 第 7 关：引入硬砖（障碍柱），窄挡板
        LevelConfig(460f, 40f, rows(
            "DDDDDDDDDD",
            "DIDDDDDDID",
            "DDDDDDDDDD",
            "DIDDDDDDID",
            "DDDDDDDDDD",
        )),
        // 第 8 关：硬砖 + 耐久砖混合
        LevelConfig(480f, 40f, rows(
            "DDDDDDDDDD",
            "DIDDDDDDID",
            "22DDDDDD22",
            "DIDDDDDDID",
            "DDDDDDDDDD",
            "2222222222",
        )),
        // 第 9 关：硬砖增多，布局更刁钻
        LevelConfig(500f, 40f, rows(
            "DDDDDDDDDD",
            "DIIDDDDIID",
            "DDDDDDDDDD",
            "DIIDDDDIID",
            "DDDDDDDDDD",
            "DIIDDDDIID",
            "DDDDDDDDDD",
        )),
        // 第 10 关：终局，全部元素 + 高密度
        LevelConfig(520f, 40f, rows(
            "DDDDDDDDDD",
            "2222222222",
            "DIIDDDDIID",
            "DDDDDDDDDD",
            "2222222222",
            "DIIDDDDIID",
            "DDDDDDDDDD",
            "3333333333",
        )),
    )
}
