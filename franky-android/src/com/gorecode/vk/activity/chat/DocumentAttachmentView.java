package com.gorecode.vk.activity.chat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.gorecode.vk.R;
import com.gorecode.vk.data.Document;
import com.gorecode.vk.imageloader.ImageLoader;
import com.gorecode.vk.view.WebImageView;

public class DocumentAttachmentView extends FrameLayout {
	private View mFileDocumentLayout;
	private TextView mFileDocumentNameView;

	private View mImageDocumentLayout;
	private WebImageView mImageDocumentThumbView;
	private TextView mImageDocumentNameView;

	private Document mDocument;

	private final ImageLoader mImgLoader;

	public DocumentAttachmentView(Context context, ImageLoader imgLoader) {
		super(context, null);

		mImgLoader = imgLoader;

		inflate(context, R.layout.document_attachment, this);

		setUpViews();

		setDuplicateParentStateEnabled(true);
	}

	public void setDocument(Document document) {
		mDocument = document;

		updateViews();
	}

	private void onDownloadButtonClicked() {
		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(mDocument.url));

		((Activity)getContext()).startActivity(intent);
	}

	private void updateViews() {
		boolean isImage = "png".equalsIgnoreCase(mDocument.extension) || "jpg".equalsIgnoreCase(mDocument.extension) || "jpeg".equalsIgnoreCase(mDocument.extension);

		mFileDocumentLayout.setVisibility(isImage ? GONE : VISIBLE);
		mImageDocumentLayout.setVisibility(isImage ? VISIBLE : GONE);

		TextView nameView = isImage ? mImageDocumentNameView : mFileDocumentNameView;
		
		nameView.setText(mDocument.title);

		if (isImage) {
			mImageDocumentThumbView.setImageUrl(mDocument.url);
		}
	}

	private void setUpViews() {
		mFileDocumentLayout = findViewById(R.id.document_file_layout);
		mFileDocumentLayout.setOnClickListener(mOnClickHandler);
		mFileDocumentNameView = (TextView)mFileDocumentLayout.findViewById(R.id.document_title);

		mImageDocumentLayout = findViewById(R.id.document_image_layout);
		mImageDocumentLayout.setOnClickListener(mOnClickHandler);
		mImageDocumentThumbView = (WebImageView)mImageDocumentLayout.findViewById(R.id.photo_thumb);
		mImageDocumentThumbView.setImageLoader(mImgLoader);
		mImageDocumentNameView = (TextView)mImageDocumentLayout.findViewById(R.id.document_title);
	}

	private final View.OnClickListener mOnClickHandler = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			if (v == mFileDocumentLayout || v == mImageDocumentLayout) {
				onDownloadButtonClicked();
			}
		}
	};
}
