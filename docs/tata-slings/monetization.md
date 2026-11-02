# Cochilo by Tatá Slings: how it makes money

> Draft, 2026-11-02. A plan to discuss with Tatá, not a decision. Every number marked *(estimate)*
> needs checking against the store's own reports (Nuvemshop, Google Ads, the "Caixa Tatá Slings
> 2026" sheet) before anyone relies on it.

## 1. The short answer

Cochilo should **not** try to be a standalone app business. The baby-app market is crowded
(Napper, Huckleberry, BabyCenter, Sprout, Bebê Conecta), paid acquisition for apps is expensive,
and we have one Android developer and no budget for ads.

What Tatá Slings already has is rarer: **about 1,200–1,300 orders a month** *(estimate: ~40 orders a
weekday and 100–120 over a weekend, from Tatá on 28 Oct)*, a trusted founder who is a paediatric
occupational therapist, and a box that reaches a parent in the exact weeks the baby's sleep rhythm
forms (6–18 weeks, [research §2.2](../research.md)).

So Cochilo earns money in three ways, in this order:

1. **It makes the store sell more** (the main return). Cochilo comes free with every sling, keeps
   Tatá Slings on the parent's phone for 1–2 years, and brings them back for the next product at
   the right age. Revenue shows up in the store, not in the app.
2. **Cochilo+**, a small paid upgrade for people who did not buy a sling, and an unlock for people
   who did.
3. **Tatá's knowledge, packaged**: guides and a short course on routines, contact naps and
   babywearing, sold in the store and opened in the app.

No ads, no selling data, no subscription trap. The privacy promise (*nothing leaves the phone*) is
part of what we sell, so the business model has to keep it.

## 2. Who pays, for what

| Who | What they get | Price *(proposal)* | Where the money lands |
|---|---|---|---|
| Parent who buys any sling, swaddle or kit | Cochilo+ unlocked for life, by the card in the box | Included | Store (higher conversion, higher repeat rate) |
| Parent who finds the app on Google Play | Cochilo free: one-tap logging, timeline, history, basic prediction | Free | n/a (they become store leads) |
| Same parent, upgrading | Cochilo+ for life | **R$ 39,90** one-off (launch R$ 29,90) | Google Play (15% fee) or the store (as a digital product) |
| Parent who wants guidance | Guides by Tatá ("Soneca no colo e no sling", "Rotina dos 3 aos 6 meses", "Luz, escuro e sono") | R$ 29,90–49,90 each; bundle R$ 79,90 | Store (digital product) |
| Parent who wants a person | Video consultation on babywearing, or a partner sleep consultant | R$ 150–350 *(the market rate Tatá just paid was R$ 350)* | Tatá, or a referral fee from the partner |
| Sleep consultants, doulas, TOs | "Cochilo Pro" code to give clients, with a printable report from the parent's export | R$ 19,90/month or a revenue share | Store |

Why a one-off price and not a subscription: Napper charges R$ 22,90–114,90 in Brazil
(Unlimited R$ 89,90) and Huckleberry Premium is ~€130/year in France. Brazilian parents are tired of
subscriptions; "pague uma vez, é seu" is a clear difference, and the app has no server costs to
cover, because it has no server. A one-off price below Napper's cheapest annual plan makes the
choice easy.

## 3. What is free and what is Cochilo+

The free app has to be good on its own, or nobody keeps it long enough to come back to the store.

**Free, forever**
- Start sleep / Woke up, every log type, edit and delete.
- Today, History, the 24-hour timeline.
- The next-nap prediction with its reason (the card keeps showing its numbers; that is our
  difference from the black-box apps, and hiding it behind a paywall would break the product rule
  that predictions explain themselves).
- Export and import of the backup.

**Cochilo+**
- Patterns: the 14-day actogram and the 7-day averages against the age range.
- A second baby (twins, siblings) and a second phone kept in sync by export/import.
- Day-milk / night-milk tags for pumped milk ([research §2.4](../research.md)).
- Contact-nap insights: naps in the sling, in arms, in the crib, side by side (§6 below).
- A printable report for the paediatrician or consultant (PDF from the phone, shared by the parent).
- Tatá's short notes at each age ("aos 3 meses…"), written by her, reviewed against AAP 2022.
- Themes in Tatá Slings prints.

## 4. How the unlock stays offline

Cochilo has no INTERNET permission and must keep it that way. Two unlock paths keep that true:

1. **Card in the box (main path).** Each order ships with a card (Tatá already orders printed cards
   in thousands). It carries a QR code and a short code like `TATA-7K2M-9QXR`. The code is a
   message signed with a private key that never leaves the store's laptop; the app holds the public
   key and checks the signature on the phone. No server, no account, works in airplane mode.
   - Codes can be batch-generated per print run, or per order by a small script beside the label
     tool that already runs on the Nuvemshop/Tiny data.
   - The same code can be printed on the order confirmation e-mail for people who lose the card.
   - A code works on any number of phones (both parents), which is the point: no DRM fights.
2. **Google Play purchase.** For people who did not buy a sling. Google Play Billing talks to the
   Play Store app on the device, not to our servers. **Check before building:** confirm the current
   Play Billing Library doesn't add the INTERNET permission to the merged manifest. If it does, sell
   Cochilo+ only through the store (the code path) and keep Play listing free.

Changing what the app stores (an unlock flag, more babies) means bumping `BackupFile.VERSION` and
still reading version 1 files, as AGENTS.md requires.

## 5. How Cochilo sends people to the store without becoming an ad

The app opens links in the browser (an Android intent, no permission needed). Every link carries
UTM tags (`utm_source=cochilo&utm_medium=app&utm_campaign=<moment>`) so Nuvemshop and Google
Analytics can count what the app brings in.

Rules, so it never feels like an ad:
- **At most one suggestion on screen, never on Today while the baby sleeps**, never as a pop-up,
  and always dismissable for good.
- **Only when the baby's age or the parent's own logs make it useful.** Examples:
  - 0–8 weeks, many short contact naps logged → "Sonecas no colo ajudam nessa fase. Conheça o sling
    de malha." (A product the parent is likely to want, with the reason.)
  - Rolling signs at the age the AAP says to stop swaddling → a note to *stop swaddling*, with no
    product. Safety advice never sells anything.
  - ~4 months → the ring sling or a structured carrier for a heavier baby.
  - ~6 months → the TatáTessori sensory line, teethers and play.
- A "Loja Tatá Slings" row in Settings, always there, never pushed.

## 6. Features that make Cochilo a Tatá Slings app (and not just another tracker)

These are what make the subproduct make sense. They also give Tatá things to talk about.

1. **Where the baby slept**: crib, arms, sling, pram, car. Babywearing is the brand; contact naps
   are normal in the first months, and parents want to see them without guilt. The Patterns screen
   shows "Sonecas no sling: 3 esta semana" beside the others.
2. **Walk outside with the sling** as a daylight log in one tap (daylight between 12:00 and 16:00
   helps night sleep, [research §2.3](../research.md)): "Passeio de sling, 40 min de luz do dia".
3. **Safe babywearing check (T.I.C.K.S.)** on the first use of a sling log, written by Tatá, in plain
   words, and a link to her video. Never "the app checks your baby".
4. **Ritual builder**: the steps Tatá is setting up with Dante's consultant now (feed on waking,
   activity, nappy in the room, room ready, 3–5 min in arms with the same song) as a checklist the
   parent writes once and ticks. Cheap to build, and it is what parents get from paid consultations.
5. **Notes from Tatá** at each age, short, signed "Tatá, terapeuta ocupacional e mãe do Francisco e
   do Dante".

## 7. Rough numbers *(all estimates; for deciding whether to do this, not for planning cash)*

Assumptions: 1,250 orders/month; 25% of buyers have a baby under 12 months who isn't already on
another app and scan the card; Google Ads cost per conversion R$ 90,48 (week of 17–23 Oct).

| Line | Monthly, after 6 months | Notes |
|---|---|---|
| New Cochilo users from the box | ~310 | 1,250 × 25% |
| Organic Play installs | ~150 | Store listing in Portuguese, Tatá's Instagram |
| Cochilo+ sales on Play (5% of organic) | ~8 × R$ 39,90 ≈ **R$ 270** | Small on purpose; this isn't the main line |
| Guides (2% of active users) | ~10 × R$ 49,90 ≈ **R$ 500** | Grows with the content library |
| **Extra repeat orders from the app** | if 3% of ~1,500 active users buy again per month: ~45 orders | At an average ticket of R$ X, that is 45 × X. **This is the line that matters**; the store knows X |
| Ads saved | 45 repeat orders not bought through Google Ads ≈ **R$ 4.000** at R$ 90/conversion | Upper bound; some would have come back anyway |

The app's own sales will be small. The case for building it is retention and lower ad spend. If
after 3 months the UTM data shows fewer than ~20 orders a month from Cochilo, stop investing in
store features and keep it as a free gift with the sling.

## 8. Costs

- Google Play developer account: US$ 25 once (the account must be the company's, Tatá Slings'
  CNPJ, so the listing reads "Tatá Slings").
- Printing the code on the existing insert card: close to zero if it's added to the next print run.
- A privacy policy page on tataslings.com.br (needed for Play; short, because we collect nothing).
- Time: Gabriel for the app; Tatá for the notes, guides and videos (the scarce resource; plan
  around her, not around the code).
- Taxes: digital products and app income under the store's regime (Simples Nacional, and the new
  CNPJ under Lucro Presumido being set up); check with Raynan before selling guides.

## 9. Order of work

| Phase | When *(proposal)* | What | Done when |
|---|---|---|---|
| 0. Brand | Nov 2026 | Tatá Slings layer in the design system ([design-system.md](design-system.md)), name, icon, Play listing copy, privacy page | Tatá approves the look and the copy |
| 1. Soft launch | Dec 2026 | Free app on Play under Tatá Slings; "where the baby slept" log; store links with UTMs; card in the box with the Play link | 100 installs from cards; no crash reports from testers |
| 2. Cochilo+ | Jan–Feb 2027 | Offline signed codes; Cochilo+ features; Play purchase if the permission check passes | First 50 unlocks; first Play sale |
| 3. Content | Mar–Apr 2027 | First 2 guides; notes by age; ritual builder | First 20 guide sales |
| 4. Partners | from May 2027 | Cochilo Pro codes for consultants, doulas, TOs; printable report | 5 partners using it with clients |
| Review | every 3 months | UTM orders, unlocks, guide sales, Play rating | Continue, change or stop, by the numbers |

## 10. What we will not do

- Ads, trackers, analytics SDKs or a login. They'd break the promise and add LGPD work.
- Push notifications that nag ("Você esqueceu de registrar!"). A tired parent doesn't need guilt.
- Claims that the app makes babies sleep, prevents SIDS, or monitors anything. Cochilo is a diary
  with a good guess, not a medical or safety device.
- Selling products on safety screens.
- A subscription for features that cost us nothing to run.
