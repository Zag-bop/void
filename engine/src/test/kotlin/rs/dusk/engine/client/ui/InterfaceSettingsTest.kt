package rs.dusk.engine.client.ui

import rs.dusk.engine.client.ui.detail.InterfaceDetail

internal class InterfaceSettingsTest : InterfaceSendTest() {

    override fun sendStrings(name: String, component: String): Boolean {
        return manager.sendSetting(name, component, 12, 34, 56)
    }

    override fun sendIds(id: Int, component: Int): Boolean {
        return manager.sendSetting(id, component, 12, 34, 56)
    }

    override fun expected(inter: InterfaceDetail, component: Int) {
        io.sendSettings(inter, component, 12, 34, 56)
    }

}
