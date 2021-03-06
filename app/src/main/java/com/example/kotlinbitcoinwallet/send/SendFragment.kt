package com.example.kotlinbitcoinwallet.send

import FeePriority
import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.example.kotlinbitcoinwallet.MainActivity
import com.example.kotlinbitcoinwallet.R
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.zxing.integration.android.IntentIntegrator
import com.journeyapps.barcodescanner.CaptureActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import org.bitcoinj.core.*
import org.bitcoinj.params.TestNet3Params
import org.bitcoinj.wallet.SendRequest
import org.bitcoinj.wallet.Wallet
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class SendFragment : Fragment(), PopupMenu.OnMenuItemClickListener{



    private lateinit var viewModel: SendViewModel
    private lateinit var sendTxt:EditText
    private lateinit var amountTxt:EditText
    private lateinit var scanBtn:Button
    private lateinit var sendBtn:Button
    private lateinit var wallet: Wallet
    private lateinit var peerGroup: PeerGroup
    private lateinit var feeTxt: TextView
    private lateinit var txIDTxt:TextView
    private lateinit var feePriority: FeePriority
    private lateinit var fee:Coin
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
       
        val root = inflater.inflate(R.layout.send_fragment, container, false)
        sendTxt = root.findViewById(R.id.ev_address)
        amountTxt = root.findViewById(R.id.ev_amount)
        feeTxt = root.findViewById(R.id.tv_fee)
        scanBtn = root.findViewById(R.id.btn_scan)
        sendBtn = root.findViewById(R.id.btn_send)
        txIDTxt = root.findViewById(R.id.tv_txID)
        wallet = (activity as MainActivity).getWallet()
        peerGroup = (activity as MainActivity).getPeerGroup()

        viewModel = ViewModelProvider(this).get(SendViewModel::class.java)
        CoroutineScope(IO).launch {
            feePriority = generateFeePriority("https://mempool.space/api/v1/fees/recommended")
            fee = Coin.valueOf(feePriority.medFee)

        }

        return root
    }



    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        feeTxt.text = "Choose your fee"



        scanBtn.setOnClickListener{

            val scanner = IntentIntegrator(this.activity)
            scanQRCode()
        }
        sendBtn.setOnClickListener{

            confirmDialogue()

        }
        //feeTxt.text= "${feeTxt.text}  ${feePriority.medFee}"
    feeTxt.setOnClickListener{
        feePopup(feeTxt)
    }
       

    }
    private fun scanQRCode(){
        val integrator = IntentIntegrator.forSupportFragment(this).apply {
            captureActivity = CaptureActivity::class.java
            setOrientationLocked(false)
            setBeepEnabled(false)
            setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES)
            setPrompt("Scanning Address")
        }
        Log.e("Scanner","Scanning")
        integrator.initiateScan()
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents == null) Toast.makeText(this.activity, "Cancelled", Toast.LENGTH_LONG).show()
            else {


                sendTxt.text = SpannableStringBuilder(parseQR(result.contents))
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun parseQR(contents: String): String {
        var addy:String = ""
        val regex:Regex
        //TODO Accommodate with qr codes that don't start with the address ^ means starting with
        val mainNetPattern = "(bc1|[13])[a-zA-HJ-NP-Z0-9]{25,39}\$"
        val testNetPattern = "(tb1|[nm2])[a-zA-HJ-NP-Z0-9]{25,39}\$"
        regex = if(wallet.params is TestNet3Params) Regex(testNetPattern)
        else Regex(mainNetPattern)


        if(regex.containsMatchIn(contents)){
           val match = regex.find(contents)
            addy =  match!!.value
            Toast.makeText(this.activity, "Scanned: $contents", Toast.LENGTH_LONG).show()


        }
        else{
            Toast.makeText(this.activity, "Invalid Address: $contents", Toast.LENGTH_LONG).show()
        }
        return addy
    }
    private fun confirmDialogue(){
        try {


            val sendAddress = Address.fromString(wallet.networkParameters, StringBuilder(sendTxt.text).toString())

            val amount = Coin.parseCoinInexact(StringBuilder(amountTxt.text).toString())



            val sendAddressStr: String = "To: " + StringBuilder(sendTxt.text).toString()
            val amountStr: String = "Amount: ${amount.toPlainString()} BTC"
            val feeStr = "Fee: ${fee.toPlainString()} BTC"
            val finalStr = "Final Amount: ${amount.plus(fee).toPlainString()} BTC"
            val alertDialog = AlertDialog.Builder(this.requireContext())
                    .setTitle("Confirm Your Request")
                    .setMessage("Check your Transaction Details: \n $sendAddressStr \n $amountStr \n $feeStr \n $finalStr ")
                    .setPositiveButton("SEND") { _, _ ->


                        sendTransaction(sendAddress, amount)


                    }
                    .setNegativeButton("CANCEL") { _, _ ->

                    }.create()
            alertDialog.show()
        }catch (e:Exception){
            Toast.makeText(context, "Please fill out all fields correctly!", Toast.LENGTH_SHORT).show()
        }
    }
    private fun sendTransaction(sendAddress: Address, finalCoin:Coin){

        Toast.makeText(context,"Broadcasting Transaction", Toast.LENGTH_SHORT).show()
        try {
            val request = SendRequest.to(sendAddress, finalCoin)
            request.feePerKb = fee
            wallet.sendCoins(request)
            txIDTxt.text = request.tx.txId.toString()
            Toast.makeText(context,"Broadcasted!", Toast.LENGTH_SHORT).show()
        } catch (e:InsufficientMoneyException){
            Toast.makeText(this.requireContext(), "Not Enough Money in Balance", Toast.LENGTH_LONG).show()
        }catch (e:Wallet.DustySendRequested){
            Toast.makeText(this.requireContext(), "Transaction must be at least ${Transaction.MIN_NONDUST_OUTPUT.toPlainString()} BTC", Toast.LENGTH_LONG).show()
        } catch (e:Exception){
            Toast.makeText(this.requireContext(), "Amm: ${StringBuilder(amountTxt.text)} \n Addr: $sendAddress is invalid", Toast.LENGTH_LONG).show()
        }


    }
    private fun feePopup(v:View){
    val feePopup = PopupMenu(context,v)
        feePopup.setOnMenuItemClickListener(this)
        feePopup.inflate(R.menu.fees)
        feePopup.show()
    }
    override fun onMenuItemClick(item: MenuItem?): Boolean {

       when(item!!.itemId){
           R.id.high_fee ->{
               fee= Coin.valueOf(feePriority.highFee)
               feeTxt.text = SpannableStringBuilder("${item.title} ${fee.toPlainString()} BTC")
               return true
           }
           R.id.low_fee ->{
               fee = Coin.valueOf(feePriority.lowFee)
               feeTxt.text = SpannableStringBuilder("${item.title} ${fee.toPlainString()} BTC")
               return true
           }
           R.id.med_fee -> {
                fee= Coin.valueOf(feePriority.medFee)
                feeTxt.text = SpannableStringBuilder("${item.title} ${fee.toPlainString()} BTC")
                return true
           }
           else -> {
               return false
           }

       }


    }

    private fun generateFeePriority(feeUrl: String): FeePriority {
        val response = URL(feeUrl).readText()
        val gson = Gson()
        return gson.fromJson(response, FeePriority::class.java)
    }
}