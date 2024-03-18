const functions = require('firebase-functions');

const admin = require('firebase-admin');
admin.initializeApp();

const spawn = require('child-process-promise').spawn;
const path = require('path');
const os = require('os');
const fs = require('fs');
var gf = require('geofire');

exports.makeUppercase = functions.firestore.document("/ImagePosts/{documentId}")
    .onCreate((snapshot, context) => {
    const original = snapshot.data();
    console.log('Uppercasing '+original.description);
    const uppercase = original.description.toUpperCase();
    return snapshot.ref.set({description: uppercase}, {merge: true});
    });

const runtimeOpts = {
    timeoutSeconds: 300,
    memory: '1GB'
}

 exports.generateResized = functions.runWith(runtimeOpts).storage.object('images').onFinalize(async (object) => {
    const fileBucket = object.bucket;
    const filePath = object.name;
    const contentType = object.contentType;

    const fileName = path.basename(filePath);

    if(fileName.indexOf("200x200_")>=0){
        return console.log(fileName+' is already resized');
    }

    const bucket = admin.storage().bucket(fileBucket);
    const tempFilePath = path.join(os.tmpdir(), fileName);
    const metadata = {
        contentType: contentType,
    }

    console.log("using only object "+object.metadata["locationKey"]);
    const uid = object.metadata["uid"];
    if(uid === null){
        return console.log("Image metadata is missing");
    }

    const photoLat = object.metadata["photoLat"];
    if(photoLat === null){
        return console.log("Image metadata is missing");
    }

    const photoLng = object.metadata["photoLng"];
    if(photoLng === null){
        return console.log("Image metadata is missing");
    }

    const desc = object.metadata["description"];
    if(desc === null){
        return console.log("Image metadata is missing");
    }

    await bucket.file(filePath).download({destination: tempFilePath});
    console.log('Image downloaded locally to ', tempFilePath);
    await spawn('convert', [tempFilePath, '-auto-orient', '-thumbnail', '200x200>', tempFilePath]);
    console.log('Thumbnail created at ', tempFilePath);

    const thumbFileName = '200x200_${fileName}';
    const thumbFilePath = path.join(path.dirname(filePath), thumbFileName);

    await bucket.upload(tempFilePath, {
        destination: thumbFilePath,
        metadata: metadata,
    });

    var posts = admin.firestore().collection("Posts");
    var postData={
        url: thumbFileName,
        likecount: 0,
        uid: uid,
        lat: photoLat,
        lng: photoLng,
        description: desc,
        timestamp: admin.firestore.FieldValue.serverTimestamp()
    };

    const res = await posts.add(postData);
    var firebaseGeofireRef = admin.database().ref("geofire/");
    var geoFire = new gf.GeoFire(firebaseGeofireRef);
    console.log(" Geofire reference created");
    await geoFire.set(res.id, [parseFloat(photoLat), parseFloat(photoLng)]);
    console.log(res.id+" Geofire inserted.");

    return fs.unlinkSync(tempFilePath);
 })