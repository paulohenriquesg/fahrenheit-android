package com.paulohenriquesg.fahrenheit.update

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The system installer gives no answer back: the user may cancel it, or Fire TV
 * may refuse for want of the unknown-sources permission. All we can do is note
 * what we handed over and see, next launch, whether it took.
 */
class PendingInstallTest {

    @Test
    fun `nothing was pending, so nothing to say`() =
        assertEquals(PendingInstall.Outcome.Nothing, PendingInstall.outcome(pendingVersionCode = null, installedVersionCode = 10))

    @Test
    fun `the install took effect`() =
        assertEquals(PendingInstall.Outcome.Installed, PendingInstall.outcome(pendingVersionCode = 12, installedVersionCode = 12))

    @Test
    fun `a later version than the one we fetched still counts as installed`() =
        assertEquals(PendingInstall.Outcome.Installed, PendingInstall.outcome(pendingVersionCode = 12, installedVersionCode = 13))

    @Test
    fun `the version did not move, so the install never happened`() =
        assertEquals(PendingInstall.Outcome.Failed, PendingInstall.outcome(pendingVersionCode = 12, installedVersionCode = 10))
}
