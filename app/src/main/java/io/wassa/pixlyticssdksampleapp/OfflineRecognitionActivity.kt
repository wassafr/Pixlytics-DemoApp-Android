package io.wassa.pixlyticssdksampleapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.NonNull
import com.google.android.material.snackbar.Snackbar
import com.tbruyelle.rxpermissions2.RxPermissions
import io.wassa.pixlytics_sdk.offline.RecognitionResult
import io.wassa.pixlytics_sdk.offline.RecognitionSession
import io.wassa.pixlytics_sdk.offline.listener.OnLoadModelListener
import io.wassa.pixlytics_sdk.offline.listener.OnRecognitionResultListener
import kotlinx.android.synthetic.main.activity_online_recognition.*
import pl.aprilapps.easyphotopicker.*

/**
 *
 * This class provide an example to who perform an image recognition with an offline model.
 */
class OfflineRecognitionActivity : AppCompatActivity() {

    private val easyImage = EasyImage.Builder(this)
        .setChooserTitle("Select image")
        .setChooserType(ChooserType.CAMERA_AND_GALLERY)
        .allowMultiple(false)
        .build()

    private var image: Bitmap? = null
    private val session = RecognitionSession()

    /**
     * On create activity initialize all button and request first page of items
     */
    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_online_recognition)

        takePictureButton.setOnClickListener { onTakePictureClick() }

        recognizeButton.setOnClickListener { onRecognizeClick() }

        progressBar.visibility = View.VISIBLE
        RxPermissions(this).request(Manifest.permission.WRITE_EXTERNAL_STORAGE).subscribe {
            if (it) {
                session.loadModel(object : OnLoadModelListener {
                    override fun onSuccess() {
                        runOnUiThread {
                            displayMessage("Model has been loaded")
                            progressBar.visibility = View.GONE
                        }
                    }

                    override fun onError(error: Throwable) {
                        runOnUiThread {
                            Log.e(TAG, "Unable to load model", error)
                            displayMessage("Unable to load model")
                            progressBar.visibility = View.GONE
                        }
                    }
                })
            }
        }

    }

    /*******************************************************************************************************************
     ***                                        TAKE PICTURE                                                         ***
     ******************************************************************************************************************/

    /**
     * Before doing any recognition we need a picture. This method ask user to get a picture from camera or gallery.
     *
     * In this demo we will use RxPermissions and EasyImage to manage permissions request and ask for a picture.
     *
     * @see <a href="https://github.com/jkwiecien/EasyImage">EasyImage</a>
     * @see <a href="https://github.com/tbruyelle/RxPermissions">RxPermissions</a>
     */
    private fun onTakePictureClick() {
        RxPermissions(this).request(android.Manifest.permission.CAMERA).subscribe {
            if (it)
                easyImage.openChooser(this)
        }
    }

    /**
     * Once the user select a picture we need to convert it into bitmap to use it with Pixlytics.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        easyImage.handleActivityResult(requestCode, resultCode, data, this, object : DefaultCallback() {
            override fun onMediaFilesPicked(imageFiles: Array<MediaFile>, source: MediaSource) {
                val imageList = mutableListOf<Bitmap>()
                imageFiles.forEach {
                    val options = BitmapFactory.Options()
                    options.inPreferredConfig = Bitmap.Config.ARGB_8888
                    imageList.add(BitmapFactory.decodeFile(it.file.absolutePath, options))
                }
                image = imageList[0]
                imageView2.setImageBitmap(image)
            }

            override fun onImagePickerError(@NonNull error: Throwable, @NonNull source: MediaSource) {
                Log.e(ItemEditingActivity.TAG, "Unable retrieve image to perform recognition", error)
            }

            override fun onCanceled(@NonNull source: MediaSource) {
                //Not necessary to remove any files manually anymore
            }
        })
    }

    /*******************************************************************************************************************
     ***                                        RECOGNIZE PICTURE                                                    ***
     ******************************************************************************************************************/

    /**
     * To recognize picture we just need to send the bitmap.
     */
    private fun onRecognizeClick() {
        if (image == null) {
            displayMessage("You must select a picture before recognition")
            return
        }
        progressBar.visibility = View.VISIBLE
        session.recognizeImage(image!!, object : OnRecognitionResultListener {
            override fun onError(error: Throwable) {
                runOnUiThread {
                    displayMessage("Unable to recognize image")
                    Log.e(OnlineRecognitionActivity.TAG, "Unable to recognize image", error)
                    progressBar.visibility = View.GONE
                }
            }

            override fun onSuccess(result: RecognitionResult) {
                runOnUiThread {
                    displayMessage("Your image as been recognize as " + result.itemId)
                    progressBar.visibility = View.GONE
                }
            }

        })
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
