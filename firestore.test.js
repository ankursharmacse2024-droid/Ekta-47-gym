const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_facility_01";
const BOB_UID = "bob_facility_02";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function validMemberPayload(now = new Date(Date.now() - 1000)) {
  return {
    memberCode: "APX-1042",
    fullName: "Marcus Vance",
    email: "marcus@apex.fit",
    phone: "+15550101042",
    tier: "BLACK_CARD",
    status: "ACTIVE",
    daysRemaining: 28,
    monthlyFeeCents: 14900,
    lockerNumber: "L-14",
    trainerName: "Devon Brooks",
    accessEnabled: true,
    autoRenew: true,
    totalCheckIns: 42,
    createdAt: now,
    updatedAt: now,
  };
}

test("Unauthenticated user cannot read or write any facility data", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("members").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("members")
      .doc("mem_1")
      .set(validMemberPayload())
  );
});

test("Facility owner can create, read, list, update, and delete members with Kotlin-aligned payloads", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  // Create facility profile
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      facilityName: "Apex Pro Downtown",
      managerName: "Alex Rivera",
      seeded: true,
      dailyCheckInTarget: 180,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Create member without @DocumentId `id` field
  const memberRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("members")
    .doc("mem_1042");
  await assertSucceeds(memberRef.set(validMemberPayload(created)));

  // List members (user-scoped subcollection)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("members").get()
  );

  // Partial update: check-in counter & lastCheckInAt
  await assertSucceeds(
    memberRef.update({
      totalCheckIns: 43,
      lastCheckInAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Partial update: renewal
  await assertSucceeds(
    memberRef.update({
      status: "ACTIVE",
      daysRemaining: 58,
      accessEnabled: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Create check-in log
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("checkins")
      .doc("chk_1")
      .set({
        memberId: "mem_1042",
        memberCode: "APX-1042",
        memberName: "Marcus Vance",
        tier: "BLACK_CARD",
        gateName: "Main Turnstile A",
        outcome: "GRANTED",
        note: "VIP access verified",
        checkedInAt: new Date(Date.now() - 1000),
        createdAt: new Date(Date.now() - 1000),
      })
  );

  // Create and update trainer
  const trainerRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("trainers")
    .doc("trn_1");
  await assertSucceeds(
    trainerRef.set({
      fullName: "Devon Brooks",
      specialty: "Strength & Conditioning",
      activeClients: 14,
      sessionsToday: 4,
      onFloor: true,
      phone: "+15550199001",
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    trainerRef.update({
      sessionsToday: 5,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Rejects @DocumentId 'id' in payload, future timestamps, shadow fields, and immutable createdAt mutations", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const membersCol = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("members");

  // Extra `id` field rejected
  await assertFails(
    membersCol.doc("mem_bad_id").set({
      id: "mem_bad_id",
      ...validMemberPayload(created),
    })
  );

  // Shadow field rejected
  await assertFails(
    membersCol.doc("mem_shadow").set({
      ...validMemberPayload(created),
      isAdmin: true,
    })
  );

  // Future timestamp rejected
  await assertFails(
    membersCol
      .doc("mem_future")
      .set(validMemberPayload(new Date(Date.now() + 60000)))
  );

  // Invalid tier enum rejected
  await assertFails(
    membersCol.doc("mem_bad_enum").set({
      ...validMemberPayload(created),
      tier: "GOLD_VIP",
    })
  );

  // Create valid member then reject createdAt mutation
  await assertSucceeds(membersCol.doc("mem_ok").set(validMemberPayload(created)));
  await assertFails(
    membersCol.doc("mem_ok").update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Cross-user isolation: Bob cannot read or modify Alice's members, checkins, or trainers", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db
      .collection("users")
      .doc(ALICE_UID)
      .collection("members")
      .doc("mem_alice")
      .set(validMemberPayload());
    await db
      .collection("users")
      .doc(ALICE_UID)
      .collection("trainers")
      .doc("trn_alice")
      .set({
        fullName: "Devon Brooks",
        specialty: "HIIT & Mobility",
        activeClients: 10,
        sessionsToday: 2,
        onFloor: true,
        phone: "+15550199001",
        createdAt: new Date(Date.now() - 5000),
        updatedAt: new Date(Date.now() - 5000),
      });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("members")
      .doc("mem_alice")
      .get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("members").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("members")
      .doc("mem_alice")
      .update({
        status: "EXPIRED",
        updatedAt: new Date(Date.now() - 1000),
      })
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("trainers")
      .doc("trn_alice")
      .delete()
  );
});

test("Revenue history: owner CRUD and query succeed; rejects unauthenticated, cross-user, negative, and immutable mutations", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const created = new Date(Date.now() - 10000);

  const validMonth = {
    monthKey: "2026-11",
    monthLabel: "Nov",
    sortOrder: 202611,
    collectedCents: 65400,
    outstandingCents: 14900,
    autoPayCents: 39700,
    activeMembers: 5,
    createdAt: created,
    updatedAt: created,
  };

  // Owner create & ordered list
  const docRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("revenue_history")
    .doc("2026-11");
  await assertSucceeds(docRef.set(validMonth));
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("revenue_history")
      .orderBy("sortOrder", "asc")
      .get()
  );

  // Owner partial update
  await assertSucceeds(
    docRef.update({
      collectedCents: 75300,
      outstandingCents: 5000,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Rejects negative cents, immutable monthKey mutation, unauth, and cross-user
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("revenue_history")
      .doc("2026-10")
      .set({ ...validMonth, monthKey: "2026-10", collectedCents: -100 })
  );
  await assertFails(
    docRef.update({
      monthKey: "2026-12",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("revenue_history").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("revenue_history").get()
  );
});

test("Member profile photo URL/path: supports HTTPS URLs and clearing, rejects Base64 data URIs and cross-user updates", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const membersCol = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("members");

  // Create member with Firebase Storage photo URL and path
  const photoMemberRef = membersCol.doc("mem_photo_1");
  await assertSucceeds(
    photoMemberRef.set({
      ...validMemberPayload(created),
      profilePhotoUrl: "https://firebasestorage.googleapis.com/v0/b/ekta47/o/avatar.jpg?alt=media",
      profilePhotoPath: "members/mem_photo_1/profile/avatar_123.jpg",
    })
  );

  // Update only profilePhotoUrl, profilePhotoPath, updatedAt
  await assertSucceeds(
    photoMemberRef.update({
      profilePhotoUrl: "https://firebasestorage.googleapis.com/v0/b/ekta47/o/avatar2.jpg?alt=media",
      profilePhotoPath: "members/mem_photo_1/profile/avatar_456.jpg",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Remove photo by clearing fields to empty string
  await assertSucceeds(
    photoMemberRef.update({
      profilePhotoUrl: "",
      profilePhotoPath: "",
      updatedAt: new Date(Date.now() - 250),
    })
  );

  // Reject Base64 data URI inside Firestore
  await assertFails(
    photoMemberRef.update({
      profilePhotoUrl: "data:image/jpeg;base64,/9j/4AAQSkZJRg==",
      profilePhotoPath: "members/mem_photo_1/profile/bad.jpg",
      updatedAt: new Date(Date.now() - 100),
    })
  );

  // Reject oversized URL (>1000 chars)
  await assertFails(
    photoMemberRef.update({
      profilePhotoUrl: "https://example.com/" + "a".repeat(1000),
      profilePhotoPath: "members/mem_photo_1/profile/long.jpg",
      updatedAt: new Date(Date.now() - 100),
    })
  );

  // Cross-user photo update denied
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("members")
      .doc("mem_photo_1")
      .update({
        profilePhotoUrl: "https://example.com/hijack.jpg",
        profilePhotoPath: "members/mem_photo_1/profile/hijack.jpg",
        updatedAt: new Date(Date.now() - 100),
      })
  );
});

test("Admin gym settings, membership plans, notifications, payments, immutable transactions, and STAFF restrictions", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const aliceUserRef = aliceDb.collection("users").doc(ALICE_UID);

  // Create ADMIN workspace profile
  await assertSucceeds(
    aliceUserRef.set({
      userId: ALICE_UID,
      facilityName: "EKTA 47 GYM",
      managerName: "Admin User",
      seeded: true,
      dailyCheckInTarget: 180,
      activeRole: "ADMIN",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Create gym_settings
  const gymSettingsRef = aliceUserRef.collection("gym_settings").doc("default");
  await assertSucceeds(
    gymSettingsRef.set({
      gymName: "EKTA 47 GYM & FITNESS CENTRE",
      logoUrl: "https://firebasestorage.googleapis.com/v0/b/ekta47/o/logo.jpg?alt=media",
      logoPath: "gym/" + ALICE_UID + "/branding/logo_1.jpg",
      adminName: "Admin User",
      contactPhone: "+91 98765 00000",
      contactEmail: "admin@ekta47.fit",
      address: "Sector 47, Gurugram",
      openingHours: "5:30 AM - 10:00 PM",
      currency: "INR",
      memberIdPrefix: "EKTA-",
      startingMemberId: 1728,
      nextMemberId: 1728,
      idCardTagline: "SINCE 1986 • OFFICIAL DIGITAL PASS",
      idCardShowLogo: true,
      idCardShowLocker: true,
      idCardShowCoach: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject Base64 data URL in gym_settings
  await assertFails(
    gymSettingsRef.update({
      logoUrl: "data:image/png;base64,iVBORw0KGgo=",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Create membership_plans as ADMIN
  const planRef = aliceUserRef.collection("membership_plans").doc("plan_monthly");
  await assertSucceeds(
    planRef.set({
      name: "Monthly",
      durationMonths: 1,
      priceCents: 150000,
      description: "Full floor access",
      isActive: true,
      isDefault: true,
      sortOrder: 1,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Create notification_settings & payment_settings as ADMIN
  const notifRef = aliceUserRef.collection("notification_settings").doc("default");
  await assertSucceeds(
    notifRef.set({
      expiry15Days: true,
      expiry7Days: true,
      expiry3Days: true,
      expiry1Day: true,
      expiredReminder: true,
      whatsappEnabled: true,
      expiringTemplate: "Hi {memberName}, expires {expiryDate} for {amount}.",
      expiredTemplate: "Hi {memberName}, expired {expiryDate}.",
      renewalTemplate: "Thank you {memberName}! Renewed till {expiryDate} ({amount}).",
      createdAt: created,
      updatedAt: created,
    })
  );

  const payRef = aliceUserRef.collection("payment_settings").doc("default");
  await assertSucceeds(
    payRef.set({
      acceptCash: true,
      acceptUpi: true,
      acceptOnline: false,
      upiId: "ekta47@okicici",
      upiQrCodeUrl: "https://firebasestorage.googleapis.com/v0/b/ekta47/o/qr.jpg?alt=media",
      upiQrCodePath: "gym/" + ALICE_UID + "/payments/upi_qr_1.jpg",
      paymentInstructions: "Pay via Cash or UPI",
      onlinePaymentEnabled: false,
      onlineGatewayLabel: "External Gateway",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Create immutable transaction snapshot
  const txRef = aliceUserRef.collection("membership_transactions").doc("tx_1");
  await assertSucceeds(
    txRef.set({
      memberId: "mem_1",
      memberCode: "EKTA-1728",
      memberName: "Aarav Mehta",
      planId: "plan_monthly",
      planName: "Monthly",
      durationMonths: 1,
      durationDays: 30,
      baseFeeCents: 150000,
      discountCents: 10000,
      chargedAmountCents: 140000,
      paymentMethod: "UPI",
      transactionRef: "UTR123456789",
      transactionType: "NEW_MEMBERSHIP",
      startDateFormatted: "24 Nov 2026",
      expiryDateFormatted: "24 Dec 2026",
      notes: "Launch discount",
      createdAt: created,
    })
  );

  // Transactions cannot be mutated (immutable ledger)
  await assertFails(
    txRef.update({
      chargedAmountCents: 50000,
    })
  );

  // Switch workspace mode to STAFF: staff cannot modify plans, settings, or delete records
  await assertSucceeds(
    aliceUserRef.update({
      activeRole: "STAFF",
      updatedAt: new Date(Date.now() - 400),
    })
  );

  await assertFails(
    planRef.update({
      priceCents: 100,
      updatedAt: new Date(Date.now() - 300),
    })
  );
  await assertFails(
    payRef.update({
      upiId: "hijack@upi",
      updatedAt: new Date(Date.now() - 300),
    })
  );
  await assertFails(
    notifRef.update({
      whatsappEnabled: false,
      updatedAt: new Date(Date.now() - 300),
    })
  );
  await assertFails(
    gymSettingsRef.update({
      gymName: "Changed By Staff",
      updatedAt: new Date(Date.now() - 300),
    })
  );

  // STAFF can still advance nextMemberId during registration and log new immutable transactions
  await assertSucceeds(
    gymSettingsRef.update({
      nextMemberId: 1729,
      updatedAt: new Date(Date.now() - 250),
    })
  );
  await assertSucceeds(
    aliceUserRef.collection("membership_transactions").doc("tx_2").set({
      memberId: "mem_2",
      memberCode: "EKTA-1729",
      memberName: "Kiran Rao",
      planId: "plan_monthly",
      planName: "Monthly",
      durationMonths: 1,
      durationDays: 30,
      baseFeeCents: 150000,
      discountCents: 0,
      chargedAmountCents: 150000,
      paymentMethod: "CASH",
      transactionRef: "",
      transactionType: "NEW_MEMBERSHIP",
      startDateFormatted: "24 Nov 2026",
      expiryDateFormatted: "24 Dec 2026",
      notes: "",
      createdAt: created,
    })
  );

  // STAFF cannot delete member records
  await assertSucceeds(
    aliceUserRef.collection("members").doc("mem_staff_check").set(validMemberPayload(created))
  );
  await assertFails(
    aliceUserRef.collection("members").doc("mem_staff_check").delete()
  );

  // Cross-user access remains denied
  await assertSucceeds(aliceUserRef.collection("membership_plans").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("membership_plans").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("payment_settings").doc("default").get()
  );

  // Owner can switch activeRole back to ADMIN
  await assertSucceeds(
    aliceUserRef.update({
      activeRole: "ADMIN",
      updatedAt: new Date(Date.now() - 100),
    })
  );
  await assertSucceeds(
    planRef.update({
      priceCents: 180000,
      updatedAt: new Date(Date.now() - 50),
    })
  );
});



