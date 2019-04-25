package io.wassa.pixlyticssdksampleapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import io.wassa.pixlytics_sdk.common.Pixlytics
import io.wassa.pixlytics_sdk.online.listener.OnGetAllItemsListener
import io.wassa.pixlytics_sdk.online.listener.OnRemoveItemsListener
import io.wassa.pixlytics_sdk.online.model.ModelItem
import io.wassa.pixlytics_sdk.online.model.Page
import io.wassa.pixlytics_sdk.online.model.filter
import kotlinx.android.synthetic.main.activity_item_editing.*
import android.text.InputType
import android.widget.EditText
import com.tbruyelle.rxpermissions2.RxPermissions
import pl.aprilapps.easyphotopicker.ChooserType
import pl.aprilapps.easyphotopicker.EasyImage
import pl.aprilapps.easyphotopicker.MediaSource
import pl.aprilapps.easyphotopicker.MediaFile
import pl.aprilapps.easyphotopicker.DefaultCallback
import android.content.Intent
import android.graphics.Bitmap
import androidx.annotation.NonNull
import android.graphics.BitmapFactory
import io.wassa.pixlytics_sdk.online.listener.OnAddItemListener
import io.wassa.pixlytics_sdk.online.listener.OnTrainItemsListener

/**
 * This activity provide example for items management (creation, remove, model train)
 */
class ItemEditingActivity : AppCompatActivity() {

    private val modelItemsAdapter = ModelItemAdapter()

    // ------ Use to retrieve item ---------
    private var currentPage = 1
    private var isLoading = false
    private var noMoreResult = false
    // -------------------------------------

    // ------ Use to add item --------------
    private val easyImage = EasyImage.Builder(this)
        .setChooserTitle("Select image")
        .setChooserType(ChooserType.CAMERA_AND_GALLERY)
        .allowMultiple(true)
        .build()
    private var itemName: String = ""
    // -------------------------------------

    /**
     * On create activity initialize all button and request first page of items
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_item_editing)

        recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@ItemEditingActivity)
            adapter = modelItemsAdapter
            addOnScrollListener(mScrollListener)
        }

        removeButton.setOnClickListener { onRemoveClick() }
        addButton.setOnClickListener { onAddClick() }
        trainButton.setOnClickListener { onTrainClick() }

        swipeToRefresh.setOnRefreshListener {
            resetItems()
            requestItems()
        }

        requestItems()
    }

    /*******************************************************************************************************************
     ***                                            TRAIN MODEL                                                      ***
     ******************************************************************************************************************/

    /**
     * To train a model the [Pixlytics.trainModel] method only need a list of items to put inside the model.
     * All the items pass in parameter would be able to be recognize.
     *
     * [ModelItemAdapter.itemsSelected] contains the list of all the items selected in the recycler view.
     * @see ModelItemAdapter
     */
    private fun onTrainClick() {
        swipeToRefresh.isRefreshing = true
        Pixlytics.trainModel(modelItemsAdapter.itemsSelected, object : OnTrainItemsListener {
            override fun onError(error: Throwable) {
                swipeToRefresh.isRefreshing = false
                displayMessage("Unable to train items")
            }

            override fun onSuccess() {
                displayMessage("Items has been train")
                resetItems()
                requestItems()
            }

        })
    }

    /*******************************************************************************************************************
     ***                                            ADD ITEMS                                                        ***
     ******************************************************************************************************************/

    /**
     * To add items on the items database we need some information.
     * First we need an item name, next a collection of images to recognize the item and last a default image to display.
     *
     * In this demo we will use the first image of the collection as default image.
     *
     * In this demo we will use RxPermissions and EasyImage to manage permissions request and ask for an images collection.
     *
     * @see <a href="https://github.com/jkwiecien/EasyImage">EasyImage</a>
     * @see <a href="https://github.com/tbruyelle/RxPermissions">RxPermissions</a>
     *
     * -----------------------------------------------------------
     *
     * On add button is clicked we open a dialog to ask for a name.
     * Next go to [selectImages] to ask for images collection
     *
     * @see selectImages
     *
     */
    private fun onAddClick() {
        AlertDialog.Builder(this).apply {
            setTitle("Enter item name")
            val editText = EditText(this@ItemEditingActivity)
            editText.inputType = InputType.TYPE_CLASS_TEXT
            setView(editText)
            setPositiveButton(
                "Select pictures"
            ) { dialog, which ->
                itemName = editText.text.toString()
                selectImages()
            }
            setNegativeButton(
                "Cancel"
            ) { dialog, which -> dialog.cancel() }
            show()
        }
    }

    /**
     * Select picture with easyImage after permissions check.
     *
     * @see <a href="https://github.com/jkwiecien/EasyImage">EasyImage</a>
     * @see <a href="https://github.com/tbruyelle/RxPermissions">RxPermissions</a>
     *
     * Allow multiple selection.
     *
     * Result is handle in [onActivityResult]
     */
    private fun selectImages() {
        RxPermissions(this).request(android.Manifest.permission.CAMERA).subscribe {
            if (it)
                easyImage.openGallery(this)
        }
    }

    /**
     * On activity result, use easyImage to retrieve images files.
     *
     * We need to convert image files before use them as bitmap in Pixlytics.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        easyImage.handleActivityResult(requestCode, resultCode, data, this, object : DefaultCallback() {
            override fun onMediaFilesPicked(imageFiles: Array<MediaFile>, source: MediaSource) {
                swipeToRefresh.isRefreshing = true
                val imageBitmaps = mutableListOf<Bitmap>()
                // Convert image file in bitmap
                imageFiles.forEach {
                    imageBitmaps.add(
                        BitmapFactory.decodeFile(
                            it.file.absolutePath,
                            BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 })
                    )
                }
                // Add items
                Pixlytics.addModelItem(itemName, imageBitmaps, object : OnAddItemListener {
                    override fun onError(error: Throwable) {
                        swipeToRefresh.isRefreshing = false
                        Log.e(TAG, "Unable to add items", error)
                        displayMessage("Unable to add items")
                    }

                    override fun onSuccess(item: ModelItem) {
                        swipeToRefresh.isRefreshing = false
                        displayMessage("Items has been added")
                        resetItems()
                        requestItems()
                    }

                }, imageBitmaps[0]) // Use the first image of the collection as default image
            }

            override fun onImagePickerError(@NonNull error: Throwable, @NonNull source: MediaSource) {
                Log.e(TAG, "Unable to add model items", error)
            }

            override fun onCanceled(@NonNull source: MediaSource) {}
        })
    }

    /*******************************************************************************************************************
     ***                                            REMOVE ITEMS                                                     ***
     ******************************************************************************************************************/

    /**
     * To remove items we only need an item or a list of items.
     *
     * In this demo we will just retrieve all items selected.
     *
     * @see ModelItemAdapter.itemsSelected
     */
    private fun onRemoveClick() {
        swipeToRefresh.isRefreshing = true
        Pixlytics.removeModelItems(modelItemsAdapter.itemsSelected, object : OnRemoveItemsListener {
            override fun onError(error: Throwable) {
                Log.e(TAG, "Unable to remove model items", error)
                displayMessage("Unable to remove items")
                swipeToRefresh.isRefreshing = false
            }

            override fun onSuccess() {
                resetItems()
                requestItems()
                displayMessage("Items has been removed")
                swipeToRefresh.isRefreshing = false
            }
        })
    }

    /*******************************************************************************************************************
     ***                                            RETRIEVE ITEMS                                                   ***
     ******************************************************************************************************************/

    /**
     * This function is use to retrieve items base on the filter.
     *
     * Warning ! this feature is paged.
     */
    fun requestItems() {
        isLoading = true
        swipeToRefresh.isRefreshing = true
        Pixlytics.getModelItems(object : OnGetAllItemsListener {
            override fun onError(error: Throwable) {
                Log.e(TAG, "Unable to retrieve items from Pixlytics server", error)
                displayMessage("Unable to retrieve items from Pixlytics server")
                isLoading = false
            }

            override fun onSuccess(page: Page<List<ModelItem>>) {
                swipeToRefresh.isRefreshing = false
                currentPage++ // On success increase the current page to retrieve the next one in future.
                if (currentPage >= page.totalPage)
                    noMoreResult = true
                isLoading = false
                modelItemsAdapter.addItems(page.data)
            }

        }, filter {
            page = currentPage
            isDelete = false
        })
    }

    /**
     * On reset all items are remove from the adapter,
     * the current page is reset to the first and all status boolean are reset.
     */
    fun resetItems() {
        modelItemsAdapter.reset()
        currentPage = 1
        isLoading = false
        noMoreResult = false
    }

    /**
     * Listener use to retrieve next page once the user reach the bottom of the current.
     */
    private val mScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            val visibleItemCount = recyclerView.layoutManager?.childCount
            val totalItemCount = recyclerView.layoutManager?.itemCount
            val pastVisibleItems = (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
            if (visibleItemCount == null || totalItemCount == null)
                return
            if (visibleItemCount + pastVisibleItems >= totalItemCount && !isLoading && !noMoreResult) {
                requestItems()
            }
        }
    }

    /*******************************************************************************************************************
     ***                                            UTILS                                                            ***
     ******************************************************************************************************************/

    fun displayMessage(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show()
    }


    companion object {
        val TAG = this::class.simpleName
    }
}
