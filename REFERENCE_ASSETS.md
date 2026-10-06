# Reference artwork

The PNG assets in `app/src/main/res/drawable-nodpi` are extracted from the screenshots supplied for this project. Planner illustrations, service tiles, railway photographs, social banner, profile/wallet/account artwork, language button and navigation/filter icons reuse those visible source images. Navigation icons have a transparent background so selection can change their tint.

These are screenshot-derived raster assets, not the original vector/source artwork. They retain the supplied image resolution and compression detail. No screenshot status bar, personal name, ticket reference, balance or date is used as app data. The social banner includes the platform logos visible in the reference; accounts/links are not configured.

All five information cards are now included. Three additional photographs were cropped from the later screenshots supplied by the user. Captions reproduce those references.

| Resource | Source | Crop pixels (left, top, right, bottom) |
| --- | --- | --- |
| fact_noney | 1000153095.jpg | 105, 1315, 474, 1643 |
| fact_hubballi | 1000153095.jpg | 509, 1315, 878, 1643 |
| fact_electrification | 1000153097.jpg | 500, 1315, 869, 1643 |

## Measured crops

```json
[
  {
    "resource": "planner_reserved",
    "source": "03-1000151748.jpg",
    "crop_pixels": [
      24,
      454,
      291,
      678
    ]
  },
  {
    "resource": "planner_unreserved",
    "source": "03-1000151748.jpg",
    "crop_pixels": [
      328,
      454,
      595,
      678
    ]
  },
  {
    "resource": "planner_platform",
    "source": "03-1000151748.jpg",
    "crop_pixels": [
      631,
      454,
      898,
      678
    ]
  },
  {
    "resource": "service_search",
    "source": "03-1000151748.jpg",
    "crop_pixels": [
      24,
      870,
      195,
      1025
    ]
  },
  {
    "resource": "service_pnr",
    "source": "03-1000151748.jpg",
    "crop_pixels": [
      258,
      870,
      429,
      1025
    ]
  },
  {
    "resource": "service_coach",
    "source": "03-1000151748.jpg",
    "crop_pixels": [
      493,
      870,
      664,
      1025
    ]
  },
  {
    "resource": "service_track",
    "source": "03-1000151748.jpg",
    "crop_pixels": [
      727,
      870,
      898,
      1025
    ]
  },
  {
    "resource": "service_food",
    "source": "03-1000151748.jpg",
    "crop_pixels": [
      24,
      1214,
      195,
      1368
    ]
  },
  {
    "resource": "service_refund",
    "source": "03-1000151748.jpg",
    "crop_pixels": [
      258,
      1214,
      429,
      1368
    ]
  },
  {
    "resource": "service_help",
    "source": "03-1000151748.jpg",
    "crop_pixels": [
      493,
      1214,
      664,
      1368
    ]
  },
  {
    "resource": "service_waves",
    "source": "03-1000151748.jpg",
    "crop_pixels": [
      727,
      1214,
      898,
      1368
    ]
  },
  {
    "resource": "fact_first_train",
    "source": "04-1000151750.jpg",
    "crop_pixels": [
      26,
      632,
      395,
      959
    ]
  },
  {
    "resource": "fact_chenab",
    "source": "04-1000151750.jpg",
    "crop_pixels": [
      431,
      632,
      800,
      959
    ]
  },
  {
    "resource": "social_banner",
    "source": "04-1000151750.jpg",
    "crop_pixels": [
      46,
      1328,
      876,
      1718
    ]
  },
  {
    "resource": "profile_avatar",
    "source": "05-1000151752.jpg",
    "crop_pixels": [
      369,
      282,
      553,
      466
    ]
  },
  {
    "resource": "wallet_green",
    "source": "06-1000151754.jpg",
    "crop_pixels": [
      104,
      421,
      172,
      482
    ]
  },
  {
    "resource": "wallet_violet",
    "source": "07-1000151756.jpg",
    "crop_pixels": [
      215,
      630,
      287,
      698
    ]
  },
  {
    "resource": "profile_password",
    "source": "06-1000151754.jpg",
    "crop_pixels": [
      136,
      1245,
      211,
      1320
    ]
  },
  {
    "resource": "profile_account",
    "source": "06-1000151754.jpg",
    "crop_pixels": [
      423,
      1250,
      494,
      1320
    ]
  },
  {
    "resource": "profile_transfer",
    "source": "06-1000151754.jpg",
    "crop_pixels": [
      137,
      1514,
      212,
      1582
    ]
  },
  {
    "resource": "profile_transactions",
    "source": "06-1000151754.jpg",
    "crop_pixels": [
      432,
      1507,
      490,
      1584
    ]
  },
  {
    "resource": "profile_aadhaar",
    "source": "06-1000151754.jpg",
    "crop_pixels": [
      715,
      1510,
      782,
      1585
    ]
  },
  {
    "resource": "passenger_avatar",
    "source": "06-1000151754.jpg",
    "crop_pixels": [
      89,
      991,
      165,
      1069
    ]
  },
  {
    "resource": "language_button",
    "source": "03-1000151748.jpg",
    "crop_pixels": [
      39,
      106,
      135,
      202
    ]
  }
]
```

## Passenger gender artwork

Three transparent gender icons are extracted from 1000153103.jpg: male [88,541,152,610], female [268,541,334,610], trans [444,539,516,611]. White background pixels were removed; UI tint indicates selection. All other bottom-panel fields, preference chips, dietary markers and close controls are rendered from app state.

## Launch and bookings references

| Resource | Supplied source | Extraction / derivation |
| --- | --- | --- |
| app_icon | 1000153272.jpg | Blue icon crop (270, 835, 652, 1216), outside black pixels made transparent; density-specific launcher copies and adaptive wrapper |
| splash_mark | app_icon | Transparent 576px canvas with centred 296px mark, within native splash masking area |
| launch_brand | 1000153268.jpg | Train and Rail One wordmark crop (270, 888, 657, 1139) |
| booking_empty | 1000153343.jpg | Grey empty ticket crop (344, 874, 572, 1028) |
| booking_sort | 1000153343.jpg | Header sort icon crop (838, 121, 901, 174) |
| booking_filter_completed / cancelled / all | Existing booking_filter_active | Ticket fill recoloured green / red / blue; outline retained |

The login layout follows 1000153271.jpg; the status colours/card layout and sorting sheet follow 1000153341.jpg, 1000153339.jpg and 1000153345.jpg. Native launcher/splash masks vary by Android version and launcher. No reference status bar or phone-specific navigation bar is embedded in these assets.

## About references

1000153620.jpg supplies the About colour/spacing, contact actions and social arrangement. 1000153622.jpg, 1000153624.jpg and 1000153626.jpg supply the scrollable legal-page layout. These screens are native Compose layouts; social marks are drawn in code and CRIS attribution uses text. Legal content is original project-specific text, with an external official policy link. 1000153618.jpg supplies the mail recipient, subject and editable body placeholder. The sending account is chosen by the installed mail app.

## Completed-ticket invoice reference

The uploaded `DOC-20261006-WA0012.pdf` supplied the A4 invoice layout, field hierarchy, watermarks and blue Indian Railways mark. `invoice_railways.png` and `invoice_watermark.png` were extracted from this user-provided reference. The generated PDF fills fields from the saved ticket and retains both reference-only and computer-generated notices. No PDF reference personal data is hard-coded into the app.
