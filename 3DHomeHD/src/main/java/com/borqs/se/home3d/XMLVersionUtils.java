package com.borqs.se.home3d;

public class XMLVersionUtils {
    
//    public static void upgradeXMLToVersion44(Context context, SQLiteDatabase db) {
//        final int oldXMLVersion = SettingsActivity.getXMLVersion(context);
//        db.beginTransaction();
//        switch (oldXMLVersion) {
//        case 0:
//            upgradeXMLToVersion1(context, db);
//        case 1:
//            upgradeXMLToVersion2(context, db);
//        case 2:
//            upgradeXMLToVersion3(context, db);
//        }
//        upgradeXMLToVersion43(context, db);
//        db.setTransactionSuccessful();
//        db.endTransaction();
//    }
//
//    private static void upgradeXMLToVersion1(Context context, SQLiteDatabase db) {
//        String table = ProviderUtils.Tables.SCENE_INFO;
//        ContentValues values = new ContentValues();
//        values.put("wall_spanX", 4);
//        values.put("wall_spanY", 4);
//        values.put("wall_sizeX", 195);
//        values.put("wall_sizeY", 234);
//        db.update(table, values, null, null);
//        upgradeModelInfo("group_bookshelf01", db, 4, 2);
//        upgradeModelInfo("group_calendar", db, 2, 3);
//        upgradeModelInfo("group_clock", db, 2, 2);
//        upgradeModelInfo("group_heart", db, 4, 2);
//        upgradeModelInfo("group_tv", db, 4, 2);
//        upgradeModelInfo("group_largewallpicframe", db, 2, 2);
//        upgradeModelInfo("group_hengwallpicframe", db, 2, 2);
//        upgradeModelInfo("group_navigation", db, 4, 3);
//        upgradeSlot(context, db);
//    }
//
//    private static void upgradeModelInfo(String name, SQLiteDatabase db, int spanX, int spanY) {
//        String where = "name='" + name + "'";
//        String table = ProviderUtils.Tables.MODEL_INFO;
//        ContentValues values = new ContentValues();
//        values.put("spanX", spanX);
//        values.put("spanY", spanY);
//        db.update(table, values, where, null);
//    }
//
//    private static void upgradeSlot(Context context, SQLiteDatabase db) {
//        String table = "Wall_Slot";
//        for (int i = 0; i < 8; i++) {
//            if (HomeUtils.DEBUG)
//                Log.d(HomeUtils.TAG, "upgradeSlot face : " + i);
//            List<ObjectSlot> existentSlot = new ArrayList<ObjectInfo.ObjectSlot>();
//            String where = "slot_Index=" + i;
//            String[] columns = { "_id" };
//            Cursor cursor = db.query(table, columns, where, null, null, null, null);
//            if (cursor != null) {
//                while (cursor.moveToNext()) {
//                    int id = cursor.getInt(0);
//                    where = "_id=" + id + " AND slotType=1";
//                    Cursor objCursor = db.query("Objects_Config", new String[] { ObjectInfoColumns.OBJECT_NAME,
//                            ObjectInfoColumns.COMPONENT_NAME }, where, null, null, null, null);
//                    if (objCursor != null) {
//                        if (objCursor.moveToFirst()) {
//                            String name = objCursor.getString(0);
//                            String componentName = objCursor.getString(1);
//                            int[] span = getSpan(context, name, componentName);
//                            if (span != null) {
//                                if (HomeUtils.DEBUG)
//                                    Log.d(HomeUtils.TAG, "name : " + name + " | " + id + " | new span : " + span[0]
//                                            + ", " + span[1]);
//                                List<ObjectSlot> emptySlots = searchEmptySlot(i, span[0], span[1], existentSlot);
//                                if (emptySlots != null && emptySlots.size() > 0) {
//                                    ObjectSlot slot = emptySlots.get(0);
//                                    if (HomeUtils.DEBUG)
//                                        Log.d(HomeUtils.TAG,
//                                                "new slot : " + slot.toString() + " | " + emptySlots.size());
//                                    where = ObjectInfoColumns._ID + "=" + id;
//                                    ContentValues slotValues = new ContentValues();
//                                    slotValues.put("slot_Index", slot.mSlotIndex);
//                                    slotValues.put("slot_StartX", slot.mStartX);
//                                    slotValues.put("slot_StartY", slot.mStartY);
//                                    slotValues.put("slot_SpanX", slot.mSpanX);
//                                    slotValues.put("slot_SpanY", slot.mSpanY);
//                                    db.update("Wall_Slot", slotValues, where, null);
//                                    existentSlot.add(slot);
//                                } else {
//                                    where = "_id=" + id;
//                                    db.delete("Objects_Config", where, null);
//                                }
//                            } else {
//                                where = "_id=" + id;
//                                db.delete("Objects_Config", where, null);
//                            }
//                        }
//                        objCursor.close();
//                    }
//                }
//                cursor.close();
//            }
//        }
//    }
//
//    private static int[] getSpan(Context context, String name, String componentName) {
//        int[] newSpan = null;
//        if (TextUtils.isEmpty(name)) {
//            return newSpan;
//        }
//        if ("group_clock".equals(name) || "group_hengwallpicframe".equals(name)
//                || "group_largewallpicframe".equals(name)) {
//            newSpan = new int[] { 2, 2 };
//        } else if ("group_bookshelf01".equals(name) || "group_tv".equals(name) || "group_heart".equals(name)) {
//            newSpan = new int[] { 4, 2 };
//        } else if ("group_calendar".equals(name)) {
//            newSpan = new int[] { 2, 3 };
//        } else if ("group_navigation".equals(name)) {
//            newSpan = new int[] { 4, 3 };
//        } else if (name.startsWith("app_") || name.startsWith("shortcut_")) {
//            newSpan = new int[] { 1, 1 };
//        } else if (name.startsWith("widget_") && !TextUtils.isEmpty(componentName)) {
//            ComponentName cn = ComponentName.unflattenFromString(componentName);
//            if (cn != null) {
//                AppWidgetProviderInfo widget = WidgetObject.findWidgetByComponent(context, cn);
//                if (widget != null) {
//                    newSpan = HomeUtils.getSpanForWidget(context, widget);
//                }
//            }
//        }
//        return newSpan;
//    }
//
//    private static List<ObjectSlot> searchEmptySlot(int faceIndex, int spanX, int spanY, List<ObjectSlot> existentSlot) {
//        int sizeX = 4;
//        int sizeY = 4;
//        boolean[][] slot = new boolean[sizeY][sizeX];
//
//        for (int y = 0; y < sizeY; y++) {
//            for (int x = 0; x < sizeX; x++) {
//                slot[y][x] = true;
//            }
//        }
//        for (ObjectSlot objectSlot : existentSlot) {
//            int startY = objectSlot.mStartY;
//            int startX = objectSlot.mStartX;
//            float sX = objectSlot.mSpanX;
//            float sY = objectSlot.mSpanY;
//            for (int y = startY; y < startY + sY; y++) {
//                if (y < sizeY) {
//                    for (int x = startX; x < startX + sX; x++) {
//                        if (x < sizeX) {
//                            slot[y][x] = false;
//                        }
//                    }
//                }
//            }
//        }
//        return searchEmptySlot(faceIndex, spanX, spanY, slot);
//    }
//
//    private static List<ObjectSlot> searchEmptySlot(int faceIndex, int spanX, int spanY, boolean[][] slot) {
//        int sizeX = 4;
//        int sizeY = 4;
//        List<ObjectSlot> objectSlots = null;
//        for (int j = 0; j <= sizeY - spanY; j++) {
//            for (int i = 0; i <= sizeX - spanX; i++) {
//                boolean hasSlot = true;
//                for (int n = j; n < j + spanY; n++) {
//                    for (int m = i; m < i + spanX; m++) {
//                        if (!slot[n][m]) {
//                            hasSlot = false;
//                            break;
//                        }
//                    }
//                    if (!hasSlot)
//                        break;
//                }
//                if (hasSlot) {
//                    ObjectSlot objectSlot = new ObjectSlot();
//                    objectSlot.mSlotIndex = faceIndex;
//                    objectSlot.mStartX = i;
//                    objectSlot.mStartY = j;
//                    objectSlot.mSpanX = spanX;
//                    objectSlot.mSpanY = spanY;
//                    if (objectSlots == null) {
//                        objectSlots = new ArrayList<ObjectSlot>();
//                    }
//                    objectSlots.add(objectSlot);
//                }
//            }
//        }
//        return objectSlots;
//    }
//
//    private static void upgradeXMLToVersion2(Context context, SQLiteDatabase db) {
//        String where = "name LIKE 'widget_%' AND (slot_SpanX<1 OR slot_SpanY<1)";
//        String table = "Objects_Config LEFT JOIN Wall_Slot USING(_id) "
//                + "LEFT JOIN Desk_Slot USING(_id) LEFT JOIN Wall_Gap_Slot USING(_id)";
//        String[] columns = { "Objects_Config._id", "slot_SpanX", "slot_SpanY" };
//        Cursor cursor = db.query(table, columns, where, null, null, null, null);
//        if (cursor != null) {
//            while (cursor.moveToNext()) {
//                int id = cursor.getInt(0);
//                int spanX = cursor.getInt(1);
//                int spanY = cursor.getInt(2);
//
//                ContentValues values = new ContentValues();
//                if (spanX < 1) {
//                    values.put("slot_SpanX", 1);
//                }
//                if (spanY < 1) {
//                    values.put("slot_SpanY", 1);
//                }
//                if (values.size() > 0) {
//                    table = "Wall_Slot";
//                    where = ObjectInfoColumns._ID + "=" + id;
//                    db.update(table, values, where, null);
//                }
//            }
//        }
//    }
//
//    private static void upgradeXMLToVersion3(Context context, SQLiteDatabase db) {
//        db.beginTransaction();
//        try {
//            String table = Tables.MODEL_INFO;
//            String objectName = "group_phoneclassic";
//            String where = ModelColumns.OBJECT_NAME + "='" + objectName + "'";
//            db.delete(table, where, null);
//            table = Tables.OBJECTS_INFO;
//            where = ObjectInfoColumns.OBJECT_NAME + "='" + objectName + "'";
//            db.delete(table, where, null);
//
//            table = Tables.MODEL_INFO;
//            objectName = "group_s3blue";
//            where = ModelColumns.OBJECT_NAME + "='" + objectName + "'";
//            db.delete(table, where, null);
//            table = Tables.OBJECTS_INFO;
//            where = ObjectInfoColumns.OBJECT_NAME + "='" + objectName + "'";
//            db.delete(table, where, null);
//
//            table = Tables.SCENE_INFO;
//            where = SceneInfoColumns.SCENE_NAME + "='home'";
//            db.delete(table, where, null);
//
//            table = Tables.MODEL_INFO;
//            where = ModelColumns.SCENE_NAME + "='home'";
//            db.delete(table, where, null);
//            table = Tables.OBJECTS_INFO;
//            where = ObjectInfoColumns.SCENE_NAME + "='home'";
//            db.delete(table, where, null);
//
//            InputStream is = context.getAssets().open("base/contact/models_config.xml");
//            XmlPullParser parser = Xml.newPullParser();
//            parser.setInput(is, "utf-8");
//            XmlUtils.beginDocument(parser, "config");
//            ModelInfo config = ModelInfo.CreateFromXml(parser);
//            config.saveToDB(db);
//            is.close();
//
//            db.setTransactionSuccessful();
//        } catch (Throwable ex) {
//        } finally {
//            db.endTransaction();
//        }
//
//    }
//
//    private static void upgradeXMLToVersion43(Context context, SQLiteDatabase db) {
//
//    }
//    
//    public static class OldObjectInfo {
//        public static final int SLOT_TYPE_WALL = 1;
//        public static final int SLOT_TYPE_DESKTOP = 2;
//        public static final int SLOT_TYPE_WALL_GAP = 3;
//        public static final int SLOT_TYPE_SKY = 0;
//
//        public int mID;
//
//
//        public String mName;
//
//        public int mIndex;
//        
//        public String mRootName;
//
//        public int mRootIndex;
//
//        public int mSlotType;
//
//        public ObjectSlot mObjectSlot;
//
//       public OldObjectInfo() {
//            mSlotType = 0;
//            mObjectSlot = new ObjectSlot();
//            mRootIndex = 0;
//        }
//
//        public static OldObjectInfo CreateFromDB(Cursor cursor) {
//            OldObjectInfo info = new OldObjectInfo();
//            info.mID = cursor.getInt(cursor.getColumnIndexOrThrow("_id"));
//            info.mName = cursor.getString(cursor.getColumnIndexOrThrow("name"));
//            info.mIndex = cursor.getInt(cursor.getColumnIndexOrThrow("objectIndex"));
//            info.mRootName = cursor.getString(cursor.getColumnIndexOrThrow("rootName"));
//            info.mSlotType = cursor.getInt(cursor.getColumnIndexOrThrow("slotType"));
//            if (info.mSlotType == SLOT_TYPE_DESKTOP) {
//                info.mObjectSlot.mSlotIndex = cursor.getInt(cursor.getColumnIndexOrThrow("D_slot_Index"));
//            } else if (info.mSlotType == SLOT_TYPE_WALL) {
//                info.mObjectSlot.mSlotIndex = cursor.getInt(cursor.getColumnIndexOrThrow("slot_Index"));
//                info.mObjectSlot.mStartX = cursor.getInt(cursor.getColumnIndexOrThrow("slot_StartX"));
//                info.mObjectSlot.mStartY = cursor.getInt(cursor.getColumnIndexOrThrow("slot_StartY"));
//                info.mObjectSlot.mSpanX = cursor.getInt(cursor.getColumnIndexOrThrow("slot_SpanX"));
//                info.mObjectSlot.mSpanY = cursor.getInt(cursor.getColumnIndexOrThrow("slot_SpanY"));
//            } else if (info.mSlotType == SLOT_TYPE_WALL_GAP) {
//                info.mObjectSlot.mSlotIndex = cursor.getInt(cursor.getColumnIndexOrThrow("G_slot_Index"));
//            }
//            return info;
//        }
//
//        public static class ObjectSlot {
//            public int mSlotIndex = 0;
//            public int mStartX = 0;
//            public int mStartY = 0;
//            public int mSpanX = 0;
//            public int mSpanY = 0;
//
//            public void set(String slot) {
//                if (slot == null) {
//                    return;
//                }
//                String[] slots = slot.split(ProviderUtils.SPLIT_SYMBOL);
//                if (slots.length == 1) {
//                    mSlotIndex = Integer.parseInt(slots[0]);
//                } else if (slots.length == 5) {
//                    mSlotIndex = Integer.parseInt(slots[0]);
//                    mStartX = Integer.parseInt(slots[1]);
//                    mStartY = Integer.parseInt(slots[2]);
//                    mSpanX = Integer.parseInt(slots[3]);
//                    mSpanY = Integer.parseInt(slots[4]);
//                }
//            }
//
//            public String toString() {
//                return mSlotIndex + "," + mStartX + "," + mStartY + "," + mSpanX + "," + mSpanY;
//            }
//
//            public ObjectSlot clone() {
//                ObjectSlot newOS = new ObjectSlot();
//                newOS.mSlotIndex = mSlotIndex;
//                newOS.mStartX = mStartX;
//                newOS.mStartY = mStartY;
//                newOS.mSpanX = mSpanX;
//                newOS.mSpanY = mSpanY;
//                return newOS;
//            }
//
//            @Override
//            public boolean equals(Object o) {
//                if (o == null) {
//                    return false;
//                }
//                ObjectSlot newOP = (ObjectSlot) o;
//                if (newOP.mSlotIndex == mSlotIndex && newOP.mStartX == mStartX && newOP.mStartY == mStartY
//                        && newOP.mSpanX == mSpanX && newOP.mSpanY == mSpanY) {
//                    return true;
//                }
//                return false;
//            }
//
//        }
//        
//        public void upgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//            switch (oldVersion) {
//                case 0:
//                    break;
//            }
//        }
//    }

}
