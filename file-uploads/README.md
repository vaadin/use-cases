# File uploads — use cases

A standalone Spring Boot demo of what users want to get done by sending files to a Vaadin Flow application, and of what the application does with the files once they arrive. Each view is one realistic scenario.

| # | View | What it shows |
| - | ---- | ------------- |
| UC1 | Report damage | Report a pothole or a broken streetlight from a phone. "Take photo" opens the rear camera directly (`UploadButton#setCapture(UploadCapture.ENVIRONMENT)`), photos are previewed and can be removed, and the location is attached with the Geolocation API. |
| UC2 | Attach documents | An expense claim whose receipts wait in the browser until "Send claim", then are uploaded and processed together with the fields: the claim is only saved if every receipt is a real PDF or photo. Receipts are required, removable before sending, limited by type, count and size, and all rejected files are reported in one message. |
| UC3 | Profile picture | Show the current picture, replace or remove it. The file must really be an image of a minimum size; the server crops it to a square and scales it to 256 × 256. |
| UC4 | Photo album | Drop many photos at once or add them with a button, follow the batch in a thumbnail file list, browse the album, open a photo full size and remove single photos. Duplicate file names are kept apart. |
| UC5 | Large file | Send a file of up to 500 MB, streamed to a temporary file, with progress, time left, cancel, screen-reader announcements and a SHA-256 checksum of what arrived. |
| UC6 | Import a file | Import a product list saved from a spreadsheet as CSV. Every row is checked and problems are listed with their line number before anything is imported. |

Gaps found in the upload APIs along the way are collected in [`API-GAPS.md`](API-GAPS.md).

## Run

```
cd file-uploads
mvn spring-boot:run
```

Open <http://localhost:8080/>.

On a desktop browser, the camera button in UC1 opens the normal file chooser, because browsers ignore `capture` where there is no camera. To see the camera open, use a phone on the same network; attaching the location there needs HTTPS (or a tunnel), because browsers only offer geolocation in secure contexts.
