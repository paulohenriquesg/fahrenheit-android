package com.paulohenriquesg.fahrenheit.update

/**
 * Whether an update we handed to the system installer actually landed.
 *
 * Nothing is reported back to us: the user can cancel the installer, and on
 * Fire TV it is refused outright without the unknown-sources permission. So we
 * note what was dispatched and compare on the next launch.
 */
object PendingInstall {

    enum class Outcome { Nothing, Installed, Failed }

    fun outcome(pendingVersionCode: Int?, installedVersionCode: Int): Outcome = when {
        pendingVersionCode == null -> Outcome.Nothing
        installedVersionCode >= pendingVersionCode -> Outcome.Installed
        else -> Outcome.Failed
    }
}
