package tbcode.example.cryptotestnetwallet.utils

import android.content.Context
import io.horizontalsystems.bitcoincore.AbstractKit
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.core.Bip
import io.horizontalsystems.bitcoinkit.BitcoinKit

enum class CryptoKits {
T_BTC {




    override fun createKit(context: Context, words: List<String>): BitcoinKit {

        return BitcoinKit(
            context,
            words,
            passphrase = "",
            walletId,
            BitcoinKit.NetworkType.TestNet,
            syncMode = BitcoinCore.SyncMode.Api(),
            bip = Bip.BIP84
        )
    }

    override fun setLabel() {
        this.label = "tBTC"
    }

};
    val walletId = "MyWallet"
    var label = "tBTC"
    abstract fun createKit(context: Context, words: List<String>):AbstractKit
    abstract fun setLabel()
    init {
        setLabel()
    }

}