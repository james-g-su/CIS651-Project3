package com.example.firebaseintro;

import android.annotation.SuppressLint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class RecyclerViewAdapter extends RecyclerView.Adapter< RecyclerViewAdapter.ViewHolder> {
    ChildEventListener usersRefListener;
    private final FirebaseUser currentUser;
    private final List<String> keyList;
    private final HashMap<String,PostModel> key_to_Post;
    private Marker currentMarker =null;
    private  ItemClickListener itemClickListener;

    public RecyclerViewAdapter(HashMap<String,PostModel> kp, List<String> kl, ItemClickListener _itemClickListener){
        keyList=kl;
        itemClickListener =_itemClickListener;
        key_to_Post= kp;
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_view, parent,false);
        final ViewHolder vh = new ViewHolder(v);
        return vh;
    }
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        final PostModel u =key_to_Post.get(keyList.get(position));
        String uid=u.uid;
        if(holder.uref!=null && holder.urefListener!=null)
        {
            holder.uref.removeEventListener(holder.urefListener);
        }
        if(holder.likesRef!=null && holder.likesRefListener!=null)
        {
            holder.likesRef.removeEventListener(holder.likesRefListener);
        }
        if(holder.likeCountRef!=null && holder.likeCountRefListener!=null)
        {
            holder.likeCountRef.removeEventListener(holder.likeCountRefListener);
        }
        Picasso.get().load(u.url).into(holder.imageView);
        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        holder.uref = database.getReference("Users").child(uid);
        holder.uref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                holder.fname_v.setText("First Name: " +dataSnapshot.child("displayname").getValue().toString());
                holder.email_v.setText("Email:  " + dataSnapshot.child("email").getValue().toString());
                holder.phone_v.setText("Phone Num:  " + dataSnapshot.child("phone").getValue().toString());
                holder.date_v.setText("Date Created: "+u.date);
                if (dataSnapshot.child("profilePicture").exists()) {
                    Picasso.get().load(dataSnapshot.child("profilePicture").getValue().toString())
                            .transform(new CircleTransform()).into(holder.profileImage);
                    holder.profileImage.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        holder.likeCountRef=
                database.getReference("ImagePosts/"+u.postKey+"/likeCount");
        Log.d("LIKEC ", u.postKey);
        holder.likeCountRefListener = holder.likeCountRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //Log.d("CRASH", dataSnapshot.toString());
                if(dataSnapshot.getValue()!=null)
                    holder.likeCount.setText(dataSnapshot.getValue().toString()+" Likes");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        holder.likesRef=database.getReference("ImagePosts/"+u.postKey+"/likes/"+currentUser.getUid());
        holder.likesRefListener=holder.likesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.getValue().toString().equals("true"))
                {
                    holder.likeBtn.setImageDrawable(ContextCompat.getDrawable(holder.likeBtn.getContext(), R.drawable.like_active));
                }
                else{
                    holder.likeBtn.setImageDrawable(ContextCompat.getDrawable(holder.likeBtn.getContext(), R.drawable.like_disabled));
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        holder.likeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                database.getReference("ImagePosts/"+u.postKey).runTransaction(new Transaction.Handler() {
                    @NonNull
                    @Override
                    public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                        PhotoPreview.Post p = mutableData.getValue(PhotoPreview.Post.class);
                        if (p == null) {
                            return Transaction.success(mutableData);
                        }

                        if (p.likes.containsKey(currentUser.getUid())) {
                            // Unstar the post and remove self from stars
                            p.likeCount = p.likeCount - 1;
                            p.likes.remove(currentUser.getUid());
                        } else {
                            // Star the post and add self to stars
                            p.likeCount = p.likeCount + 1;
                            p.likes.put(currentUser.getUid(), true);
                        }

                        // Set value and report transaction success
                        mutableData.setValue(p);
                        return Transaction.success(mutableData);
                    }

                    @Override
                    public void onComplete(@Nullable DatabaseError databaseError, boolean b, @Nullable DataSnapshot dataSnapshot) {

                    }
                });
            }
        });
        holder.description_v.setText(u.description);
        holder.imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentMarker!=null)
                    currentMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.marker_grey));

                u.m.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.marker_red));
                currentMarker=u.m;
                if (itemClickListener!=null)
                    itemClickListener.onItmeClick(currentMarker.getPosition());
            }
        });
        // TODO LIST
        if(u.uid == currentUser.getUid()){
            holder.deleteBtn.setImageDrawable(ContextCompat.getDrawable(holder.deleteBtn.getContext(), R.drawable.delete_icon_foreground));
            holder.deleteBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // how to get user id TODO LIST
                    keyList.remove(position);
                    Log.d("Post removed ", u.postKey);
                }
            });
        }else{
            holder.deleteBtn.setVisibility(View.INVISIBLE);
            Log.d("User does not have permissions ", u.postKey);
        }
    }

    @Override
    public int getItemCount() {
        return keyList.size();
    }
    public static class ViewHolder extends RecyclerView.ViewHolder{
        public TextView fname_v;
        public TextView email_v;
        public TextView phone_v;
        public TextView date_v;
        public TextView description_v;
        public ImageView imageView;
        public  ImageView likeBtn;
        public ImageView deleteBtn;
        public TextView likeCount;
        DatabaseReference uref;
        ValueEventListener urefListener;
        DatabaseReference likeCountRef;
        ValueEventListener likeCountRefListener;
        DatabaseReference likesRef;
        public ImageView profileImage;
        ValueEventListener likesRefListener;
        public ViewHolder(View v){
            super(v);
            fname_v = v.findViewById(R.id.fname_view);
            email_v = v.findViewById(R.id.email_view);
            phone_v = v.findViewById(R.id.phone_view);
            date_v = v.findViewById(R.id.date_view);
            description_v=v.findViewById(R.id.description);
            profileImage = v.findViewById(R.id.userImage);
            imageView=v.findViewById(R.id.postImg);
            likeBtn=v.findViewById(R.id.likeBtn);
            deleteBtn=v.findViewById(R.id.deleteImage);
            likeCount=v.findViewById(R.id.likeCount);
        }
    }

}
