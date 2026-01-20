package xyz.vexo.commands

import com.github.stivais.commodore.Commodore
import xyz.vexo.utils.sendCommand

val entranceCommand = Commodore("e", "E", "f0", "F0") {
    runs { sendCommand("joindungeon catacombs_entrance") }
}

val f1Command = Commodore("f1", "F1") {
    runs { sendCommand("joindungeon catacombs_floor_one") }
}

val f2Command = Commodore("f2", "F2") {
    runs { sendCommand("joindungeon catacombs_floor_two") }
}

val f3Command = Commodore("f3", "F3") {
    runs { sendCommand("joindungeon catacombs_floor_three") }
}

val f4Command = Commodore("f4", "F4") {
    runs { sendCommand("joindungeon catacombs_floor_four") }
}

val f5Command = Commodore("f5", "F5") {
    runs { sendCommand("joindungeon catacombs_floor_fife") }
}

val f6Command = Commodore("f6", "F6") {
    runs { sendCommand("joindungeon catacombs_floor_six") }
}

val f7Command = Commodore("f7", "F7") {
    runs { sendCommand("joindungeon catacombs_floor_seven") }
}

val m1Command = Commodore("m1", "M1") {
    runs { sendCommand("joindungeon master_catacombs_floor_one") }
}

val m2Command = Commodore("m2", "M2") {
    runs { sendCommand("joindungeon master_catacombs_floor_two") }
}

val m3Command = Commodore("m3", "M3") {
    runs { sendCommand("joindungeon master_catacombs_floor_three") }
}

val m4Command = Commodore("m4", "M4") {
    runs { sendCommand("joindungeon master_catacombs_floor_four") }
}

val m5Command = Commodore("m5", "M5") {
    runs { sendCommand("joindungeon master_catacombs_floor_fife") }
}

val m6Command = Commodore("m6", "M6") {
    runs { sendCommand("joindungeon master_catacombs_floor_six") }
}

val m7Command = Commodore("m7", "M7") {
    runs { sendCommand("joindungeon master_catacombs_floor_seven") }
}

val f1 = Commodore("f1", "F1") {
    runs { sendCommand("joindungeon_floor_one") }
}

val f2 = Commodore("f2", "F2") {
    runs { sendCommand("joindungeon_floor_two") }
}

val f3 = Commodore("f3", "F3") {
    runs { sendCommand("joindungeon_floor_three") }
}

val f4 = Commodore("f4", "F4") {
    runs { sendCommand("joindungeon_floor_four") }
}

val f5 = Commodore("f5", "F5") {
    runs { sendCommand("joindungeon_floor_fife") }
}

val f6 = Commodore("f6", "F6") {
    runs { sendCommand("joindungeon_floor_six") }
}

val f7 = Commodore("f7", "F7") {
    runs { sendCommand("joindungeon_floor_seven") }
}

val t1Command = Commodore("t1", "T1") {
    runs { sendCommand("joininstance kuudra_normal") }
}

val t2Command = Commodore("t2", "T2") {
    runs { sendCommand("joininstance kuudra_hot") }
}

val t3Command = Commodore("t3", "T3") {
    runs { sendCommand("joininstance kuudra_burning") }
}

val t4Command = Commodore("t4", "T4") {
    runs { sendCommand("joininstance kuudra_fiery") }
}

val t5Command = Commodore("t5", "T5") {
    runs { sendCommand("joininstance kuudra_infernal") }
}