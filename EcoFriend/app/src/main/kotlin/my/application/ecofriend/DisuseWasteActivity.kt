package my.application.ecofriend

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDialog
import androidx.core.content.FileProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_direct_fill_waste.*
import kotlinx.android.synthetic.main.item_big_class.*
import my.application.ecofriend.adapters.ClassAdapter
import my.application.ecofriend.databinding.ActivityDirectFillWasteBinding
import my.application.ecofriend.databinding.ProgressDialogBinding
import my.application.ecofriend.datas.Item
import my.application.ecofriend.datas.MyData
import okhttp3.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.*
import kotlin.collections.ArrayList

class DisuseWasteActivity : AppCompatActivity() {
    private val CAMERA_PERMISSION_REQUEST = 1000
    private val GALLERY_PERMISSION_REQUEST = 1001
    private val EXTERNAL_PERMISSION_REQUEST = 1002
    private val FILE_NAME = "picture.jpg"
    private var uploadChooser: UploadChooser? = null
    private lateinit var progressDialog: AppCompatDialog

    private val mClassList = ArrayList<String>()
    private var mItem = ArrayList<Item>()

    lateinit var mClassAdapter: ClassAdapter

    val database = Firebase.database
    val myRef = database.getReference("nowon")

    private var mBinding: ActivityDirectFillWasteBinding? = null
    private val binding get() = mBinding!!
    private var mBinding2: ProgressDialogBinding? =null
    private val binding2 get() = mBinding2!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_direct_fill_waste)
        mBinding = ActivityDirectFillWasteBinding.inflate(layoutInflater)
        setContentView(binding.root)


        setupListener()
        setupValue()

    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        startActivity(Intent(this@DisuseWasteActivity, MainHomeActivity::class.java))
    }

    private fun setupValue() {

        mClassList.add("?????????????????")
        mClassList.add("????????????????????")
        mClassList.add("????????????")
        mClassList.add("????????????")
        mClassList.add("????????????")
        mClassList.add("????????????")
        mClassList.add("??????")

        mClassAdapter = ClassAdapter(this, R.layout.class_list, mClassList)

        classListView.adapter = mClassAdapter
    }

    private fun setupListener() {
        classListView.setOnItemClickListener { adapterView, view, i, l ->
            // ?????? ????????? ???????????? ????????? ?????? ???????????? ????????????
            val clickedClass = mClassList[i]
            // ????????? ??????????????? ??????????????? ?????? ?????? ??????
            val myIntent = Intent(this, GetDatabase::class.java)

            if(MyData.prefs.getString("gps", "no gps") == "??????????????? ?????????") {
                myRef.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val test = snapshot.child(clickedClass)

                        for (data in test.children) {
                            mItem.add(
                                Item(
                                    false,
                                    clickedClass,
                                    data.child("dockey").value as String,
                                    data.child("item").value as String?,
                                    data.child("standard").value as String?,
                                    data.child("levy_amt").value as Long
                                ))

                        }

                        // ????????? ????????????
                        myIntent.putExtra("mItemList", mItem)

                        // ?????? ??????
                        startActivity(myIntent)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        TODO("Not yet implemented")
                    }
                })
            }
            else{
                Toast.makeText(this, "?????? ????????? ???????????? ????????????.", Toast.LENGTH_SHORT).show()
            }

        }

        search_item.setOnClickListener{
            //apply????????? ?????? ?????? ?????? ?????? ????????? ??? ???????????? ?????? ??????.
            uploadChooser = UploadChooser().apply {
                addNotifier(object : UploadChooser.UploadChooserNotifierInterface{
                    override fun cameraOnClick() {
                        Log.d("upload", "CameraOnClick")
                        // ????????? ?????? ??????
                        checkCameraPermission()
                    }

                    override fun galleryOnClick() {
                        Log.d("upload", "galleryOnClick")
                        // ????????? ?????? ??????
                        checkGalleryPermission()
                    }
                })
            }
            uploadChooser!!.show(supportFragmentManager, "")
        }
    }

    private fun checkCameraPermission() {
        if (PermissionUtil().requestPermission(
                this,
                CAMERA_PERMISSION_REQUEST,
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            )
        ) {
            openCamera()
        }
    }

    private fun checkGalleryPermission() {
        if (PermissionUtil().requestPermission(
                this,
                GALLERY_PERMISSION_REQUEST,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            )
        ) {
            openGallery()
        }
    }

    private fun checkExternalPermission(bitmap: Bitmap) {
        if (PermissionUtil().requestPermission(
                this,
                EXTERNAL_PERMISSION_REQUEST,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        ) {
            progressON(bitmap)
        }
    }


    private fun openGallery() {
        val intent = Intent().apply {
            //image ????????? ?????? ???
            setType("image/*")
            setAction(Intent.ACTION_GET_CONTENT)
        }

        startActivityForResult(Intent.createChooser(intent, "Select a photo"),
            GALLERY_PERMISSION_REQUEST)
    }

    private fun openCamera() {
        //Uri??? ?????? ????????? ???????????? ??????(url??? ????????? ??????) -> photoUri: ????????? ????????? uri ??????
        //getUriForFile(file??? ?????? uri??? ??????????????? ??????)
        val photoUri =
            FileProvider.getUriForFile(this, "my.application.ecofriend.provider", createCameraFile())
//        val photoUri = FileProvider.getUriForFile(this,  applicationContext.packageName + ".provider", createCameraFile())

        //?????? ?????? ??? ????????? ???????????? ????????? ????????? startActivityForResult ??????
        startActivityForResult(
            Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                //?????? ?????????(EXTRA_OUTPUT)??? photoUri??? ??????
                putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }, CAMERA_PERMISSION_REQUEST
        )
    }

    //data: Intent? -> ??????????????? ???? 'null'??? ?????? ????????? ?????? ??????
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            CAMERA_PERMISSION_REQUEST -> {
                //????????? ???????????? ????????? ????????? ??? ???????????? ??????????????? ??? RESULT_OK. ????????? ???????????? ?????? ????????? ???????????? ????????? ?????????
                if (resultCode != Activity.RESULT_OK) return
                val photoUri = FileProvider.getUriForFile(this,
                    "my.application.ecofriend.provider",
                    createCameraFile())
                uploadImage(photoUri)

                //?????? ????????? ???????????? ???????????? ????????? ??? ????????? ?????? ???????????? ???.

            }
            GALLERY_PERMISSION_REQUEST -> {
                //data??? null??? ????????? let ????????? ??????
                data?.let {
                    it.data?.let { it1 -> uploadImage(it1) }
                }
            }
        }
    }


    //??????????????? ??????????????? ??????????????? ???????????? ????????????, ??????????????? ????????? ???????????? ??????????????? ???????????? ???.(???????????? ???????????? ????????? ?????? ?????? ??????)
    private fun uploadImage(imageUri: Uri) {
        //????????? ????????? ?????? ????????? ?????????. ????????? ??????????????? ?????????.
        var bitmap: Bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
        bitmap = resizeBitmap(bitmap)
        //uploaded_image.setImageBitmap(bitmap)
        uploadChooser?.dismiss()

        checkExternalPermission(bitmap)
    }

    private fun createCameraFile(): File {
        //dir??? ????????? ????????? ??????
        val dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File(dir, FILE_NAME)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            GALLERY_PERMISSION_REQUEST -> {
                if (PermissionUtil().permissionGranted(requestCode,
                        GALLERY_PERMISSION_REQUEST,
                        grantResults)
                ) {
                    openGallery()
                }
            }
            CAMERA_PERMISSION_REQUEST -> {
                //?????? ?????? ????????? ?????? ?????? ????????? ????????????, ???????????? ????????? ???????????? ????????? ??????(grantResults size???), 0?????? ????????? ??? ??? 0????????? ????????? grant ????????? ??????
                if (PermissionUtil().permissionGranted(requestCode,
                        CAMERA_PERMISSION_REQUEST,
                        grantResults)
                ) {
                    openCamera()
                }
            }
        }
    }

    fun resizeBitmap(original: Bitmap): Bitmap {
        val resizeWidth = 416;
        val resizeHeight = 416;
        val result: Bitmap = Bitmap.createScaledBitmap(original, resizeWidth, resizeHeight, true);
        val matrix = Matrix()
        matrix.postRotate(90F)
        return Bitmap.createBitmap(result, 0, 0, resizeWidth, resizeHeight, matrix, true);
    }

    fun send2Server(bitmap: Bitmap) {
        val dirPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val imageFile = File(dirPath, "/EcoFriend/${UUID.randomUUID()}.jpg")
        var str : String? = null
        var indexName: String? = null
        var indexConf: String? = null
        var os: OutputStream? = null

        try {
            if (!imageFile.isDirectory) {
                imageFile.parentFile.mkdirs()
            }
            imageFile.createNewFile()
            os = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, os)
            os.close()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        val requestBody: RequestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("photo",
                imageFile.name,                                          //"/${UUID.randomUUID()}.jpg"
                RequestBody.create(MultipartBody.FORM, imageFile))
            .build()

        val request: Request = Request.Builder()
            .url("https://capstone-design-22-3-9.df.r.appspot.com/dnn/yolo")
            // .url("http://192.168.1.101:5000/dnn/yolo") // http://127.0.0.1:5000/dnn/yolo
            .post(requestBody)
            .build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                System.out.println("Failure")
                e.printStackTrace()
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                str = response.body!!.string()
                //str = str.toString()

                //Log.d("TEST : ", response.body!!.string())
                Log.d("TEST : ", str.toString())

                var token = str!!.split('"')
                if(token.size <= 1){
                    indexName = "notwardrobe"
                }
                else{
                    indexName = token[9]
                    indexConf = token[12]
                    indexConf = indexConf!!.substring(1)
                    indexConf = indexConf!!.split('}')[0]
                }

                progressOFF()
                val intent = Intent(this@DisuseWasteActivity, deepLearningResult::class.java)
                intent.putExtra("result", indexName.toString())
                System.out.println(indexName)
                startActivity(intent)
            }
        })
    }

    fun progressON(bitmap: Bitmap){
        progressDialog = AppCompatDialog(this)
        progressDialog.setCancelable(true)
        progressDialog.getWindow()?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        //progressDialog.setContentView(R.layout.progress_dialog)
        mBinding2 = ProgressDialogBinding.inflate(layoutInflater)
        progressDialog.setContentView(binding2.root)

        progressDialog.show()
        var img_loading_framge = progressDialog.findViewById<ImageView>(R.id.iv_frame_loading)
        var frameAnimation = img_loading_framge?.getBackground() as AnimationDrawable
        img_loading_framge.post(object : Runnable {
            override fun run() {
                frameAnimation.start()
                send2Server(bitmap)
            }
        })
    }

    fun progressOFF() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss()
        }
    }

}

