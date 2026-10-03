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
