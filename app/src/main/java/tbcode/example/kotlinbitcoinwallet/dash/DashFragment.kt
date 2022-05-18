package tbcode.example.kotlinbitcoinwallet.dash

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bitcoinkit.BitcoinKit
import tbcode.example.kotlinbitcoinwallet.NumberFormatHelper
import tbcode.example.kotlinbitcoinwallet.R
import tbcode.example.kotlinbitcoinwallet.utils.KitSyncService


class DashFragment : Fragment(){



    private lateinit var viewModel: DashViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var txtBalance: TextView
    private lateinit var txtNoTransaction:TextView
    private lateinit var bitcoinKit: BitcoinKit
    private lateinit var adapter: TxAdapter
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.dash_fragment, container, false)
        recyclerView = root.findViewById(R.id.dash_recyclerview)
        txtBalance = root.findViewById(R.id.tv_Balance)
        txtNoTransaction = root.findViewById(R.id.tv_NoTransaction)
        txtNoTransaction.visibility = View.GONE
        return root
    }



    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        try {
            bitcoinKit = KitSyncService.bitcoinKit

            viewModel = ViewModelProvider(this).get(DashViewModel::class.java)

            viewModel.getBalance(bitcoinKit)
            viewModel.getTransactions(bitcoinKit)
            Log.d("DF", "Unspendable: ${bitcoinKit.balance.unspendable}")
            Log.d("DF", "Spendable: ${bitcoinKit.balance.spendable}")
            Log.d("DF", "Block-Height: ${bitcoinKit.lastBlockInfo?.height}")
            Log.d("DF", "Unspendable + Spendable: ${bitcoinKit.balance.spendable + bitcoinKit.balance.unspendable}")
            viewModel.balance.observe(viewLifecycleOwner, Observer { balance ->
                when (balance) {
                    null -> txtBalance.text = SpannableStringBuilder("0 tBTC: wallet can't be found")
                    else -> txtBalance.text = SpannableStringBuilder("${NumberFormatHelper.cryptoAmountFormat.format(balance.spendable / 100_000_000.0)} tBTC")
                }
            })
            viewModel.transactions.observe(viewLifecycleOwner, Observer {
                it?.let { transactions ->
                    if (adapter.itemCount == 0) txtNoTransaction.visibility = View.VISIBLE
                    adapter.transactions = transactions
                    adapter.notifyDataSetChanged()
                }
            })

            adapter = TxAdapter(viewModel.transactions.value, bitcoinKit.lastBlockInfo, "tBTC")
            recyclerView.adapter = adapter
            recyclerView.layoutManager = LinearLayoutManager(this.requireContext())

        } catch (e:Exception){
            txtBalance.text = SpannableStringBuilder("0.00 tBTC")
            txtNoTransaction.visibility = View.VISIBLE
        }


    }

    override fun onDestroy() {
        Log.d("btc-dash", "Dash onDestroy is called")
        super.onDestroy()
    }

}