package com.ykhapps.trialapp

import android.animation.ArgbEvaluator
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ykhapps.trialapp.models.BoardSize
import com.ykhapps.trialapp.models.MemoryGame
import com.ykhapps.trialapp.models.UserImageList
import com.ykhapps.trialapp.utils.EXTRA_BOARD_SIZE
import com.ykhapps.trialapp.utils.EXTRA_GAME_NAME
import com.github.jinatonic.confetti.CommonConfetti
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso

class MainActivity : AppCompatActivity() {

    companion object{
        private const val TAG = "MainActivity"
        private  const val CREATE_REQUEST_CODE = 888
    }
    private lateinit var clRoot: CoordinatorLayout
    private lateinit var rvBoard: RecyclerView
    private lateinit var tvNumMoves: TextView
    private lateinit var tvNumPairs: TextView

    private val db = Firebase.firestore
    private var gameName: String? = null
    private var customGameImages : List<String>? = null
    private lateinit var memoryGame: MemoryGame
    private lateinit var adapter: MemoryBoardAdapter
    private var boardSize:BoardSize = BoardSize.EASY



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        clRoot = findViewById(R.id.clRoot)
        rvBoard = findViewById(R.id.rvBoard)
        tvNumMoves = findViewById(R.id.tvNumMoves)
        tvNumPairs = findViewById(R.id.tvNumPairs)


            
//        val intent = Intent(this, CreateActivity::class.java)
//        intent.putExtra(EXTRA_BOARD_SIZE, BoardSize.EASY)
//        startActivity(intent)

        setUpBoard()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.mi_refresh ->{
                if(memoryGame.getNumMoves() > 0 && !memoryGame.haveWonGame()){
                    showAlertDialog("Quit your current game?", null, View.OnClickListener {
                        setUpBoard()
                    })
                }else{
                    setUpBoard()
                }
                return true
            }
            R.id.mi_mew_size ->{
                showNewSizeDialog()
                return true
            }
            R.id.mi_custom ->{
                showCreationDialog()
                 return true
           }
            R.id.mi_download ->{
                showDownloadDialog()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode == CREATE_REQUEST_CODE && resultCode == Activity.RESULT_OK){
            val customGameName = data?.getStringExtra(EXTRA_GAME_NAME)
            if (customGameName == null){
                Log.i(TAG, "Got null custom game from CreateActivity")
                return
            }
            downloadGame(customGameName)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }


    private fun showDownloadDialog() {
    val boardDownloadView = LayoutInflater.from(this).inflate(R.layout.dialog_download_board,null)
        showAlertDialog("Fetch memory game", boardDownloadView, View.OnClickListener {
            //Grab the text of the game name that the user wants to download
            val etDownloadGame = boardDownloadView.findViewById<EditText>(R.id.etDownloadGame)
            val gameToDownload = etDownloadGame.text.toString().trim()
            downloadGame(gameToDownload)

        })
    }

    private fun downloadGame(customGameName: String) {
    db.collection("games").document(customGameName).get().addOnSuccessListener { document ->
        val userImageList= document.toObject(UserImageList::class.java)
        if(userImageList?.images == null ){
            Log.i(TAG, "Invalid custom game data from Firebase")
            Snackbar.make(clRoot, "Sorry, we couldn't find such game, '$customGameName'", Snackbar.LENGTH_LONG).show()
            return@addOnSuccessListener
        }
        val numCards = userImageList.images.size * 2
        boardSize = BoardSize.getByValue(numCards)
        customGameImages = userImageList.images
        for(imageUrl in userImageList.images){
            Picasso.get().load(imageUrl).fetch()
        }
        Snackbar.make(clRoot, "You're now playing '$customGameName'!", Snackbar.LENGTH_LONG).show()
        gameName = customGameName
        setUpBoard()


    }.addOnFailureListener { exception ->
        Log.i(TAG, "Exception when retrieving game", exception)
    }
    }

    private fun showCreationDialog() {
        val boardSizeView =LayoutInflater.from(this).inflate(R.layout.dialog_board_size, null)
        val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)
        showAlertDialog("Create your own memory board", boardSizeView, View.OnClickListener {
            // Set a new board size
           val desiredBardSize = when(radioGroupSize.checkedRadioButtonId){
                R.id.rbEasy -> BoardSize.EASY
                R.id.rbMedium -> BoardSize.MEDIUM
                else -> BoardSize.HARD
            }
            //Navigate the user to a new activity
            val intent = Intent(this, CreateActivity ::class.java)
            intent.putExtra(EXTRA_BOARD_SIZE, desiredBardSize)
            startActivityForResult(intent, CREATE_REQUEST_CODE)
        })

    }

//    private val getResult =
//        registerForActivityResult(
//            ActivityResultContracts.StartActivityForResult()
//        ) { result->
//            if (result.resultCode== CREATE_REQUEST_CODE && result.data != null) {
//                val value = result.data?.getStringExtra("input")
//            }
//        }

    private fun showNewSizeDialog() {
        val boardSizeView =LayoutInflater.from(this).inflate(R.layout.dialog_board_size, null)
        val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)
        when(boardSize){
            BoardSize.EASY -> radioGroupSize.check(R.id.rbEasy)
            BoardSize.MEDIUM -> radioGroupSize.check(R.id.rbMedium)
            BoardSize.HARD -> radioGroupSize.check(R.id.rbHard)
        }
        showAlertDialog("Choose new size", boardSizeView, View.OnClickListener {
            // Set a new board size
            boardSize = when(radioGroupSize.checkedRadioButtonId){
                R.id.rbEasy -> BoardSize.EASY
                R.id.rbMedium -> BoardSize.MEDIUM
                else -> BoardSize.HARD
            }
            gameName = null
            customGameImages = null
            setUpBoard()

        })
    }

    private fun showAlertDialog(title: String, view:View?, positiveClickListener: View.OnClickListener) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(view)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("OK"){_,_->
                positiveClickListener.onClick(null)
            }.show()
    }

    private fun setUpBoard() {
        supportActionBar?.title = gameName ?: getString(R.string.app_name)
        memoryGame = MemoryGame(boardSize, customGameImages)
        when(boardSize){
            BoardSize.EASY -> {
                tvNumMoves.text = "EASY: 4 x 2"
                tvNumPairs.text = "Pairs: 0 / 4"
            }

            BoardSize.MEDIUM -> {
                tvNumMoves.text = "MEDIUM: 6 * 3"
                tvNumPairs.text = "Pairs: 0 / 9"
            }
            BoardSize.HARD -> {
                tvNumMoves.text = "HARD: 6 * 4"
                tvNumPairs.text = "Pairs: 0 / 12"
            }
        }

        memoryGame = MemoryGame(boardSize, customGameImages)
        // "this" parameter here is a reference to the Main Activity which is an example of context
        adapter =  MemoryBoardAdapter(this, boardSize, memoryGame.cards, object: MemoryBoardAdapter.CardClickListener{
            override fun onCardClicked(position: Int) {
                updateGameWithFlip(position)
            }
        })
        rvBoard.adapter= adapter
        rvBoard.setHasFixedSize(true)
        rvBoard.layoutManager = GridLayoutManager(this, boardSize.getWidth())
    }




    private fun updateGameWithFlip(position: Int) {
        //Checking error cases
        if(memoryGame.haveWonGame()){
            Snackbar.make(clRoot, "You already won!", Snackbar.LENGTH_LONG).show()
        return
        }
        if(memoryGame.isCardFaceUp(position)){
            Snackbar.make(clRoot, "Invalid Move", Snackbar.LENGTH_SHORT).show()
            return
        }

        if(memoryGame.flipCard(position)){
            Log.i(TAG, "Found a match! Num of pairs ${memoryGame.numPairsFound}")
            val color = ArgbEvaluator().evaluate(
                memoryGame.numPairsFound.toFloat() / boardSize.getNumPairs(),
                ContextCompat.getColor(this, R.color.color_progress_none),
                ContextCompat.getColor(this, R.color.color_progress_full)
            ) as Int
            tvNumPairs.setTextColor(color)
            tvNumPairs.text = "Pairs: ${memoryGame.numPairsFound} / ${boardSize.getNumPairs()}"
            if(memoryGame.haveWonGame()){
                Snackbar.make(clRoot, "You won! Congrats.", Snackbar.LENGTH_LONG).show()
                CommonConfetti.rainingConfetti(clRoot, intArrayOf(Color.YELLOW, Color.GREEN, Color.MAGENTA)).oneShot()
            }
        }
        tvNumMoves.text = "Moves: ${memoryGame.getNumMoves()}"
        adapter.notifyDataSetChanged()
    }
}