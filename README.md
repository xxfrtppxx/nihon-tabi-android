# nihon-tabi-android

Android app (Kotlin native) สำหรับบันทึกการท่องเที่ยวญี่ปุ่น — เริ่มที่ Android ก่อน, iOS ทำทีหลังโดยใช้ backend เดียวกัน (ดู [`../SYSTEM_DESIGN.md`](../SYSTEM_DESIGN.md))

## Stack

- Kotlin, Jetpack Compose, MVVM
- Room — offline cache ของ visits + geo data
- Retrofit + OkHttp — เรียก `nihon-tabi-api`
- MapLibre GL Native — render แผนที่ + polygon จังหวัด/เมือง
- Hilt — dependency injection
- DataStore/EncryptedSharedPreferences — เก็บ JWT token

## โครงสร้างโมดูล (แผน)

```
app/                     -- entry point, navigation graph, DI setup
core/
  network/               -- Retrofit client, API interfaces, DTO
  database/              -- Room entities/DAO (visits cache, geo cache)
  map/                    -- MapLibre setup, GeoJSON loading/styling helpers
  designsystem/           -- ธีม, สี, component ที่ใช้ร่วมกัน
feature/
  auth/                   -- login/register screen + viewmodel
  map/                     -- หน้าแผนที่หลัก (จังหวัด → เมือง) + bottom sheet mark visited
  visits/                  -- รายการที่ไปแล้ว, filter
  stats/                   -- dashboard % ที่ไปแล้ว
  profile/                 -- profile/settings
```

## Flow หลัก

1. Login/Register → เก็บ token
2. โหลด prefecture GeoJSON (bundle ติดแอพ, เบา) → แสดงแผนที่ญี่ปุ่นทั้งประเทศ สีตามสัดส่วนที่ไปแล้ว
3. แตะจังหวัด → ซูมเข้า → โหลด municipality GeoJSON ของจังหวัดนั้นจาก API (cache ใน Room) → แสดงเขตย่อย
4. แตะเมือง/เขต → bottom sheet: mark visited/want_to_go, ใส่วันที่/โน้ต/รูป/rating → บันทึกลง Room ก่อน แล้ว sync ขึ้น backend เมื่อมีเน็ต (WorkManager สำหรับ background sync)

## Offline strategy

- Visits ที่สร้าง/แก้ตอนไม่มีเน็ต เก็บใน Room พร้อม flag `pending_sync` แล้วดันขึ้น backend ด้วย WorkManager ทันทีที่ออนไลน์
- Geo data (prefectures/municipalities) cache ถาวรใน Room เพราะแทบไม่เปลี่ยน

## Setup (ยังไม่ได้เริ่ม)

เปิดเป็น Android Studio project ใหม่ (Empty Compose Activity), ตั้ง minSdk ตามความเหมาะสม (แนะนำ 26+), เพิ่ม dependency: Retrofit, Room, Hilt, MapLibre Android SDK
