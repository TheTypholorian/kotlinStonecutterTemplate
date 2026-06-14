package ${template.group.dot}.${template.id}.client

import net.typho.big_shot_lib.api.client.NeoClientInitializer
import net.typho.big_shot_lib.api.event.NeoClientEventBus

object ${template.class}Client : NeoClientInitializer {
    override val modId: String = "${template.id}"

    override fun onInitializeClient(bus: NeoClientEventBus) {
    }
}