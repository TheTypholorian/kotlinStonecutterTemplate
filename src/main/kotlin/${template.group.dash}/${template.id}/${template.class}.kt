package ${template.group.dot}.${template.id}

import net.typho.big_shot_lib.api.NeoCommonInitializer
import net.typho.big_shot_lib.api.event.NeoEventBus

object ${template.class} : NeoCommonInitializer {
    override val modId: String = "${template.id}"

    override fun onInitialize(bus: NeoEventBus) {
    }
}