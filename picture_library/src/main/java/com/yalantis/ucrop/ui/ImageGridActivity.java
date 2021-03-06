package com.yalantis.ucrop.ui;

import android.Manifest;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.yalantis.ucrop.R;
import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.adapter.ImageGridAdapter;
import com.yalantis.ucrop.compress.CompressConfig;
import com.yalantis.ucrop.compress.CompressImageOptions;
import com.yalantis.ucrop.compress.CompressInterface;
import com.yalantis.ucrop.decoration.GridSpacingItemDecoration;
import com.yalantis.ucrop.dialog.SweetAlertDialog;
import com.yalantis.ucrop.entity.Compress;
import com.yalantis.ucrop.entity.LocalMedia;
import com.yalantis.ucrop.entity.LocalMediaFolder;
import com.yalantis.ucrop.observable.ImagesObservable;
import com.yalantis.ucrop.util.PicModeConfig;
import com.yalantis.ucrop.util.FileUtils;
import com.yalantis.ucrop.util.LocalMediaLoader;
import com.yalantis.ucrop.util.ScreenUtils;
import com.yalantis.ucrop.util.ToolbarUtil;
import com.yalantis.ucrop.widget.PublicTitleBar;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * author：luck
 * project：PictureSelector
 * package：com.luck.picture.ui
 * email：893855882@qq.com
 * data：16/12/31
 */
public class ImageGridActivity extends BaseActivity implements PublicTitleBar.OnTitleBarClick, View.OnClickListener, ImageGridAdapter.OnPhotoSelectChangedListener {
    public final String TAG = ImageGridActivity.class.getSimpleName();
    private List<LocalMedia> images = new ArrayList<>();
    private RecyclerView recyclerView;
    private TextView tv_img_num;
    private TextView tv_ok;
    private RelativeLayout rl_bottom;
    private PublicTitleBar titleBar;
    private Button id_preview;
    private ImageGridAdapter adapter;
    private String cameraPath;
    private SweetAlertDialog dialog;
    private List<LocalMediaFolder> folders = new ArrayList<>();
    private List<LocalMedia> selectImages = new ArrayList<LocalMedia>();// 记录选中的图片

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_grid);
        String folderName = getIntent().getStringExtra(PicModeConfig.FOLDER_NAME);
        folders = ImagesObservable.getInstance().readLocalFolders();
        if (folders == null) {
            folders = new ArrayList<>();
        }
        type = getIntent().getIntExtra(PicModeConfig.EXTRA_TYPE, 0);// 1图片 2视频
        selectImages = (List<LocalMedia>) getIntent().getSerializableExtra(PicModeConfig.EXTRA_PREVIEW_SELECT_LIST);
        spanCount = getIntent().getIntExtra(PicModeConfig.EXTRA_MAX_SPAN_COUNT, 4);
        copyMode = getIntent().getIntExtra(PicModeConfig.EXTRA_CROP_MODE, 0);// 裁剪模式
        enableCrop = getIntent().getBooleanExtra(PicModeConfig.EXTRA_ENABLE_CROP, false);
        enablePreview = getIntent().getBooleanExtra(PicModeConfig.EXTRA_ENABLE_PREVIEW, true);// 是否预览
        showCamera = getIntent().getBooleanExtra(PicModeConfig.EXTRA_SHOW_CAMERA, true);
        selectMode = getIntent().getIntExtra(PicModeConfig.EXTRA_SELECT_MODE, PicModeConfig.MODE_MULTIPLE);
        enablePreviewVideo = getIntent().getBooleanExtra(PicModeConfig.EXTRA_ENABLE_PREVIEW_VIDEO, true);
        maxSelectNum = getIntent().getIntExtra(PicModeConfig.EXTRA_MAX_SELECT_NUM, 0);
        backgroundColor = getIntent().getIntExtra(PicModeConfig.BACKGROUND_COLOR, 0);
        cb_drawable = getIntent().getIntExtra(PicModeConfig.CHECKED_DRAWABLE, 0);
        isCompress = getIntent().getBooleanExtra(PicModeConfig.EXTRA_COMPRESS, false);
        cropW = getIntent().getIntExtra(PicModeConfig.EXTRA_CROP_W, 0);
        cropH = getIntent().getIntExtra(PicModeConfig.EXTRA_CROP_H, 0);
        definition = getIntent().getIntExtra(PicModeConfig.EXTRA_DEFINITION, PicModeConfig.HIGH);
        recordVideoSecond = getIntent().getIntExtra(PicModeConfig.EXTRA_VIDEO_SECOND, 0);
        is_checked_num = getIntent().getBooleanExtra(PicModeConfig.EXTRA_IS_CHECKED_NUM, false);
        previewColor = getIntent().getIntExtra(PicModeConfig.EXTRA_PREVIEW_COLOR, R.color.tab_color_true);
        completeColor = getIntent().getIntExtra(PicModeConfig.EXTRA_COMPLETE_COLOR, R.color.tab_color_true);
        bottomBgColor = getIntent().getIntExtra(PicModeConfig.EXTRA_BOTTOM_BG_COLOR, R.color.color_fa);
        previewBottomBgColor = getIntent().getIntExtra(PicModeConfig.EXTRA_PREVIEW_BOTTOM_BG_COLOR, R.color.bar_grey_90);
        if (savedInstanceState != null) {
            cameraPath = savedInstanceState.getString(PicModeConfig.BUNDLE_CAMERA_PATH);
        }
        images = ImagesObservable.getInstance().readLocalMedias();
        if (images == null) {
            images = new ArrayList<>();
        }
        if (selectImages == null) {
            selectImages = new ArrayList<>();
        }


        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        titleBar = (PublicTitleBar) findViewById(R.id.titleBar);
        rl_bottom = (RelativeLayout) findViewById(R.id.rl_bottom);
        titleBar.setTitleBarBackgroundColor(backgroundColor);
        ToolbarUtil.setColorNoTranslucent(this, backgroundColor);
        tv_ok = (TextView) findViewById(R.id.tv_ok);
        id_preview = (Button) findViewById(R.id.id_preview);
        tv_img_num = (TextView) findViewById(R.id.tv_img_num);
        id_preview.setOnClickListener(this);
        tv_ok.setOnClickListener(this);
        titleBar.setOnTitleBarClickListener(this);
        if (enablePreview && selectMode == PicModeConfig.MODE_MULTIPLE) {
            if (type == LocalMediaLoader.TYPE_VIDEO) {
                // 如果是视频不能预览
                id_preview.setVisibility(View.GONE);
            } else {
                id_preview.setVisibility(View.VISIBLE);
            }
        } else if (selectMode == PicModeConfig.MODE_SINGLE) {
            rl_bottom.setVisibility(View.GONE);
        } else {
            id_preview.setVisibility(View.GONE);
        }
        if (folderName != null && !folderName.equals("")) {
            titleBar.setTitleText(folderName);
        } else {
            switch (type) {
                case LocalMediaLoader.TYPE_IMAGE:
                    titleBar.setTitleText(getString(R.string.all_image));
                    break;
                case LocalMediaLoader.TYPE_VIDEO:
                    titleBar.setTitleText(getString(R.string.all_video));
                    break;
            }
        }
        rl_bottom.setBackgroundColor(bottomBgColor);
        id_preview.setTextColor(previewColor);
        tv_ok.setTextColor(completeColor);
        titleBar.setRightText(getString(R.string.cancel));
        recyclerView.setHasFixedSize(true);
        recyclerView.addItemDecoration(new GridSpacingItemDecoration(spanCount, ScreenUtils.dip2px(this, 2), false));
        recyclerView.setLayoutManager(new GridLayoutManager(this, spanCount));
        // 解决调用 notifyItemChanged 闪烁问题,取消默认动画
        ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);

        // 如果是显示数据风格，则默认为qq选择风格
        if (is_checked_num) {
            tv_img_num.setBackgroundResource(R.drawable.message_oval_blue);
            cb_drawable = R.drawable.checkbox_num_selector;
        }
        adapter = new ImageGridAdapter(this, showCamera, maxSelectNum, selectMode, enablePreview, enablePreviewVideo, cb_drawable, is_checked_num);
        recyclerView.setAdapter(adapter);
        if (selectImages.size() > 0) {
            ChangeImageNumber(selectImages);
            adapter.bindSelectImages(selectImages);
        }

        adapter.bindImagesData(images);
        adapter.setOnPhotoSelectChangedListener(ImageGridActivity.this);

    }

    @Override
    public void onClick(View view) {
        Intent intent = new Intent();
        int id = view.getId();
        if (id == R.id.id_preview) {
            List<LocalMedia> selectedImages = adapter.getSelectedImages();
            List<LocalMedia> medias = new ArrayList<>();
            for (LocalMedia media : selectedImages) {
                for (LocalMedia m : images) {
                    if (media.getPath().equals(m.getPath())) {
                        medias.add(m);
                    }
                }
            }
            intent.putExtra(PicModeConfig.EXTRA_PREVIEW_LIST, (Serializable) medias);
            intent.putExtra(PicModeConfig.EXTRA_PREVIEW_SELECT_LIST, (Serializable) selectedImages);
            intent.putExtra(PicModeConfig.EXTRA_POSITION, 0);
            intent.putExtra(PicModeConfig.EXTRA_BOTTOM_PREVIEW, true);
            intent.putExtra(PicModeConfig.EXTRA_MAX_SELECT_NUM, maxSelectNum);
            intent.putExtra(PicModeConfig.BACKGROUND_COLOR, backgroundColor);
            intent.putExtra(PicModeConfig.CHECKED_DRAWABLE, cb_drawable);
            intent.putExtra(PicModeConfig.EXTRA_IS_CHECKED_NUM, is_checked_num);
            intent.putExtra(PicModeConfig.EXTRA_COMPLETE_COLOR, completeColor);
            intent.putExtra(PicModeConfig.EXTRA_PREVIEW_BOTTOM_BG_COLOR, previewBottomBgColor);
            intent.setClass(mContext, PreviewActivity.class);
            startActivityForResult(intent, PicModeConfig.REQUEST_PREVIEW);
        } else if (id == R.id.tv_ok) {
            List<LocalMedia> images = adapter.getSelectedImages();
            // 图片才压缩，视频不管
            if (isCompress && type == LocalMediaLoader.TYPE_IMAGE) {
                ArrayList<Compress> compresses = new ArrayList<>();
                Compress compress;
                for (LocalMedia m : images) {
                    compress = new Compress();
                    compress.setPath(m.getPath());
                    compresses.add(compress);
                }
                compressImage(compresses);
            } else {
                resultBack(images);
            }
        }
    }

    private void resultBack(List<LocalMedia> images) {
        ArrayList<String> result = new ArrayList<>();
        for (LocalMedia media : images) {
            result.add(media.getPath());
        }
        if (result.size() > 0) {
            onResult(result);
        }
    }

    @Override
    public void onTakePhoto() {
        // 启动相机拍照,先判断手机是否有拍照权限
        if (hasPermission(Manifest.permission.CAMERA)) {
            startCamera();
        } else {
            requestPermission(PicModeConfig.CAMERA, Manifest.permission.CAMERA);
        }
    }

    @Override
    public void onChange(List<LocalMedia> selectImages) {
        ChangeImageNumber(selectImages);
    }

    /**
     * 图片选中数量
     *
     * @param selectImages
     */
    public void ChangeImageNumber(List<LocalMedia> selectImages) {
        Animation animation = null;
        boolean enable = selectImages.size() != 0;
        if (enable) {
            id_preview.setAlpha(1.0f);
            tv_ok.setEnabled(true);
            tv_ok.setAlpha(1.0f);
            id_preview.setEnabled(true);
            animation = AnimationUtils.loadAnimation(mContext, R.anim.modal_in);
            tv_img_num.startAnimation(animation);
            tv_img_num.setVisibility(View.VISIBLE);
            tv_img_num.setText(selectImages.size() + "");
            tv_ok.setText("已完成");
        } else {
            tv_ok.setEnabled(false);
            id_preview.setAlpha(0.5f);
            id_preview.setEnabled(false);
            tv_ok.setAlpha(0.5f);
            if (selectImages.size() > 0) {
                animation = AnimationUtils.loadAnimation(mContext, R.anim.modal_out);
                tv_img_num.startAnimation(animation);
            }
            tv_img_num.setVisibility(View.INVISIBLE);
            tv_ok.setText("请选择");
        }
    }

    @Override
    public void startCamera() {
        switch (type) {
            case LocalMediaLoader.TYPE_IMAGE:
                // 拍照
                startOpenCamera();
                break;
            case LocalMediaLoader.TYPE_VIDEO:
                // 录视频
                startOpenCameraVideo();
                break;
        }

    }

    @Override
    public void onPictureClick(LocalMedia media, int position) {
        startPreview(adapter.getImages(), position);
    }

    public void startPreview(List<LocalMedia> previewImages, int position) {
        LocalMedia media = previewImages.get(position);
        int type = media.getType();
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        switch (type) {
            case LocalMediaLoader.TYPE_IMAGE:
                if (enableCrop && selectMode == PicModeConfig.MODE_SINGLE) {
                    startCopy(media.getPath());
                } else if (!enableCrop && selectMode == PicModeConfig.MODE_SINGLE) {
                    if (isCompress) {
                        // 如果压缩图片,因为单选只能选一张，所以手动设置只压缩一次就好了
                        ArrayList<Compress> compresses = new ArrayList<>();
                        Compress compress = new Compress();
                        compress.setPath(media.getPath());
                        compresses.add(compress);
                        compressImage(compresses);
                    } else {
                        onSelectDone(media.getPath());
                    }
                } else {
                    // 图片可以预览
                    List<LocalMedia> selectedImages = adapter.getSelectedImages();
                    intent.putExtra(PicModeConfig.EXTRA_PREVIEW_SELECT_LIST, (Serializable) selectedImages);
                    intent.putExtra(PicModeConfig.EXTRA_POSITION, position);
                    intent.putExtra(PicModeConfig.EXTRA_MAX_SELECT_NUM, maxSelectNum);
                    intent.putExtra(PicModeConfig.BACKGROUND_COLOR, backgroundColor);
                    intent.putExtra(PicModeConfig.CHECKED_DRAWABLE, cb_drawable);
                    intent.putExtra(PicModeConfig.EXTRA_IS_CHECKED_NUM, is_checked_num);
                    intent.putExtra(PicModeConfig.EXTRA_COMPLETE_COLOR, completeColor);
                    intent.putExtra(PicModeConfig.EXTRA_BOTTOM_BG_COLOR, bottomBgColor);
                    intent.putExtra(PicModeConfig.EXTRA_PREVIEW_BOTTOM_BG_COLOR, previewBottomBgColor);
                    intent.setClass(mContext, PreviewActivity.class);
                    startActivityForResult(intent, PicModeConfig.REQUEST_PREVIEW);
                }
                break;
            case LocalMediaLoader.TYPE_VIDEO:
                // 视频
                if (selectMode == PicModeConfig.MODE_SINGLE) {
                    // 单选
                    onSelectDone(media.getPath());
                } else {
                    bundle.putString("video_path", media.getPath());
                    startActivity(VideoPlayActivity.class, bundle);
                }
                break;
        }

    }

    protected void startCopy(String path) {
        // 如果开启裁剪 并且是单选
        // 去裁剪
        UCrop uCrop = UCrop.of(Uri.parse(path), Uri.fromFile(new File(getCacheDir(), System.currentTimeMillis() + ".jpg")));
        UCrop.Options options = new UCrop.Options();
        switch (copyMode) {
            case PicModeConfig.COPY_MODEL_DEFAULT:
                options.withAspectRatio(0, 0);
                break;
            case PicModeConfig.COPY_MODEL_1_1:
                options.withAspectRatio(1, 1);
                break;
            case PicModeConfig.COPY_MODEL_3_2:
                options.withAspectRatio(3, 2);
                break;
            case PicModeConfig.COPY_MODEL_3_4:
                options.withAspectRatio(3, 4);
                break;
            case PicModeConfig.COPY_MODEL_16_9:
                options.withAspectRatio(16, 9);
                break;
        }
        options.withMaxResultSize(cropW, cropH);
        options.background_color(backgroundColor);
        uCrop.withOptions(options);
        uCrop.start(ImageGridActivity.this);
    }

    /**
     * start to camera、preview、crop
     */
    public void startOpenCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            File cameraFile = FileUtils.createCameraFile(this, type);
            cameraPath = cameraFile.getAbsolutePath();
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(cameraFile));
            startActivityForResult(cameraIntent, PicModeConfig.REQUEST_CAMERA);
        }
    }

    /**
     * start to camera、video
     */
    public void startOpenCameraVideo() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            File cameraFile = FileUtils.createCameraFile(this, type);
            cameraPath = cameraFile.getAbsolutePath();
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(cameraFile));
            cameraIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, recordVideoSecond);
            cameraIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, definition);
            startActivityForResult(cameraIntent, PicModeConfig.REQUEST_CAMERA);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            // on take photo success
            if (requestCode == PicModeConfig.REQUEST_CAMERA) {
                // 拍照返回
                File file = new File(cameraPath);
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
                if (selectMode == PicModeConfig.MODE_SINGLE) {
                    // 如果是单选 拍照后直接返回
                    if (enableCrop && type == LocalMediaLoader.TYPE_IMAGE) {
                        // 如果允许裁剪，并且是图片
                        startCopy(cameraPath);
                    } else {
                        if (isCompress && type == LocalMediaLoader.TYPE_IMAGE) {
                            // 压缩图片
                            ArrayList<Compress> compresses = new ArrayList<>();
                            Compress compress = new Compress();
                            compress.setPath(cameraPath);
                            compresses.add(compress);
                            compressImage(compresses);
                        } else {
                            onSelectDone(cameraPath);
                        }
                    }
                } else {
                    // 多选 返回列表并选中当前拍照的
                    int duration = 0;
                    if (type == LocalMediaLoader.TYPE_VIDEO) {
                        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                        mmr.setDataSource(file.getPath());
                        duration = Integer.parseInt(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                    } else {
                        duration = Integer.parseInt(String.valueOf(System.currentTimeMillis()).substring(0, 10));
                    }
                    LocalMedia media = new LocalMedia(file.getPath(), duration, duration, type);
                    // 根据新拍照生成的图片，插入到对应的相册当中，避免重新查询一遍数据库
                    LocalMediaFolder folder = getImageFolder(media.getPath(), folders);
                    Log.i("FolderName", folder.getName());
                    folder.getImages().add(0, media);// 插入到第一个位置
                    folder.setImageNum(folder.getImageNum() + 1);
                    folder.setFirstImagePath(media.getPath());
                    folder.setType(type);

                    List<LocalMedia> images = adapter.getImages();
                    images.add(0, media);
                    Log.i("", folders.size() + "");

                    // 没有到最大选择量 才做默认选中刚拍好的
                    if (adapter.getSelectedImages().size() < maxSelectNum) {
                        List<LocalMedia> selectedImages = adapter.getSelectedImages();
                        selectedImages.add(media);
                        adapter.bindSelectImages(selectedImages);
                        ChangeImageNumber(adapter.getSelectedImages());
                    }
                    adapter.bindImagesData(images);
                }

            } else if (requestCode == UCrop.REQUEST_CROP) {
                handleCropResult(data);
            } else if (requestCode == PicModeConfig.REQUEST_PREVIEW) {
                // 预览点击完成
                if (data != null) {
                    int type = data.getIntExtra("type", 0);
                    if (type == 1) {
                        // 返回键 返回的
                        List<LocalMedia> selectImages = (List<LocalMedia>) data.getSerializableExtra(PicModeConfig.EXTRA_PREVIEW_SELECT_LIST);
                        if (selectImages != null)
                            adapter.bindSelectImages(selectImages);
                    } else {
                        // 已完成返回
                        ArrayList<String> images = (ArrayList<String>) data.getSerializableExtra(PicModeConfig.EXTRA_PREVIEW_SELECT_LIST);
                        if (images == null)
                            images = new ArrayList<>();
                        if (isCompress && type == LocalMediaLoader.TYPE_IMAGE) {
                            ArrayList<Compress> compresses = new ArrayList<>();
                            for (String path : images) {
                                // 压缩
                                Compress compress = new Compress();
                                compress.setPath(path);
                                compresses.add(compress);
                            }
                            compressImage(compresses);
                        } else {
                            onResult(images);
                        }
                    }
                }
            }
        }
    }

    private void handleCropResult(@NonNull Intent result) {
        final Uri resultUri = UCrop.getOutput(result);
        if (resultUri != null) {
            if (isCompress && type == LocalMediaLoader.TYPE_IMAGE) {
                // 压缩图片
                ArrayList<Compress> compresses = new ArrayList<>();
                Compress compress = new Compress();
                compress.setPath(resultUri.getPath());
                compresses.add(compress);
                compressImage(compresses);
            } else {
                onSelectDone(resultUri.getPath());
            }
        }
    }

    public void onSelectDone(String path) {
        ArrayList<String> images = new ArrayList<>();
        images.add(path);
        onResult(images);
    }

    public void onResult(ArrayList<String> images) {
        setResult(RESULT_OK, new Intent().putStringArrayListExtra(PicModeConfig.REQUEST_OUTPUT, images));
        finish();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(PicModeConfig.BUNDLE_CAMERA_PATH, cameraPath);
    }


    private LocalMediaFolder getImageFolder(String path, List<LocalMediaFolder> imageFolders) {
        File imageFile = new File(path);
        File folderFile = imageFile.getParentFile();

        for (LocalMediaFolder folder : imageFolders) {
            if (folder.getName().equals(folderFile.getName())) {
                return folder;
            }
        }
        LocalMediaFolder newFolder = new LocalMediaFolder();
        newFolder.setName(folderFile.getName());
        newFolder.setPath(folderFile.getAbsolutePath());
        newFolder.setFirstImagePath(path);
        imageFolders.add(newFolder);
        return newFolder;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                List<LocalMedia> selectedImages = adapter.getSelectedImages();
                setResult(RESULT_OK, new Intent().putExtra("type", 1).putExtra(PicModeConfig.EXTRA_PREVIEW_SELECT_LIST, (Serializable) selectedImages));
                finish();
                return false;
        }
        return super.onKeyDown(keyCode, event);
    }


    @Override
    public void onLeftClick() {
        ActivityFinish();
    }

    @Override
    public void onRightClick() {
        ActivityFinish();
    }

    private void ActivityFinish() {
        setResult(RESULT_OK, new Intent().putExtra("type", 1).putExtra(PicModeConfig.EXTRA_PREVIEW_SELECT_LIST, (Serializable) adapter.getSelectedImages()));
        finish();
    }

    /**
     * 处理图片压缩
     */
    private void compressImage(ArrayList<Compress> result) {
        showDialog("处理中...");
        CompressConfig config = CompressConfig.ofDefaultConfig();
        CompressImageOptions.compress(this, config, result, new CompressInterface.CompressListener() {
            @Override
            public void onCompressSuccess(ArrayList<Compress> images) {
                // 压缩成功回调
                ArrayList<String> result = new ArrayList<String>();
                for (Compress t : images) {
                    result.add(t.getCompressPath());
                }
                onResult(result);
                if (dialog != null && dialog.isShowing()) {
                    dialog.cancel();
                }
            }

            @Override
            public void onCompressError(ArrayList<Compress> images, String msg) {
                // 压缩失败回调 返回原图
                ArrayList<String> result = new ArrayList<String>();
                List<LocalMedia> selectedImages = adapter.getSelectedImages();
                for (LocalMedia item : selectedImages) {
                    result.add(item.getPath());
                }
                onResult(result);
                if (dialog != null && dialog.isShowing()) {
                    dialog.cancel();
                }
            }
        }).compress();
    }

    private void showDialog(String msg) {
        dialog = new SweetAlertDialog(ImageGridActivity.this);
        dialog.setTitleText(msg);
        dialog.show();
    }
}
