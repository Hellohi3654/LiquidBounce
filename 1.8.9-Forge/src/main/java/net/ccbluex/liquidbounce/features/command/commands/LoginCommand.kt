package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager
import net.ccbluex.liquidbounce.utils.ServerUtils
import net.ccbluex.liquidbounce.utils.login.MinecraftAccount

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
class LoginCommand : Command("login", emptyArray()) {

    override fun execute(args: Array<String>) {
        if (args.size <= 1) {
            chatSyntax("login <username/email> [password]")
            return
        }

        val result: String = if (args.size > 2)
            GuiAltManager.login(MinecraftAccount(args[1], args[2]))
        else
            GuiAltManager.login(MinecraftAccount(args[1]))

        chat(result)

        if (result.startsWith("§cYour name is now")) {
            if (mc.isIntegratedServerRunning)
                return

            mc.theWorld.sendQuittingDisconnectingPacket()
            ServerUtils.connectToLastServer()
        }
    }

}