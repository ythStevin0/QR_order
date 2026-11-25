import { test, expect, type Page } from '@playwright/test';
import fs from 'node:fs';
import path from 'node:path';

const ARTIFACT_SCREENSHOT_DIR = path.resolve('test-results/screenshots');

async function ensureFlutterSemantics(page: Page) {
  const placeholder = page.locator('flt-semantics-placeholder');
  try {
    await placeholder.waitFor({ state: 'attached', timeout: 10_000 });
    await page.evaluate(() => {
      const el = document.querySelector('flt-semantics-placeholder') as HTMLElement | null;
      if (el) el.click();
    });
  } catch {
    // Semantics may already be active via SemanticsBinding.instance.ensureSemantics()
  }

  await expect(
    page.locator('flt-semantics', { hasText: 'Layar Dapur (KDS)' }).or(
      page.locator('[aria-label*="Layar Dapur (KDS)"]')
    ).first()
  ).toBeVisible({ timeout: 45_000 });
}

function flutterByLabelOrText(page: Page, text: string) {
  return page
    .locator(`[aria-label*="${text}"]`)
    .or(page.getByText(text))
    .first();
}

async function expectActiveOrderStep(page: Page, stepLabel: string) {
  const stepHeader = page.locator('div.justify-between', {
    has: page.getByRole('heading', { name: stepLabel, exact: true }),
  });
  await expect(stepHeader.getByText('Sekarang')).toBeVisible({ timeout: 20_000 });
}

test.describe('QR Order End-to-End Flow (Customer Web + Backend + Flutter KDS)', () => {
  test.beforeAll(() => {
    fs.mkdirSync(ARTIFACT_SCREENSHOT_DIR, { recursive: true });
  });

  test('Full order lifecycle: Takeaway checkout -> QRIS -> KDS -> SSE real-time updates -> Completed', async ({
    context,
  }) => {
    const customerName = `PW_${Date.now().toString().slice(-5)}`;
    const customerPage = await context.newPage();
    const kdsPage = await context.newPage();

    const customerErrors: string[] = [];
    const kdsErrors: string[] = [];
    customerPage.on('pageerror', (err) => customerErrors.push(err.message));
    kdsPage.on('pageerror', (err) => kdsErrors.push(err.message));

    // 1. Open Customer Web in Take Away mode
    await customerPage.goto('http://localhost:5173/?outlet=1&type=takeaway');
    await expect(customerPage.getByText('Mie Gacoan (Pedas Manis)')).toBeVisible();
    // Ensure real backend mode (no Demo Mode banner)
    await expect(customerPage.getByText(/Mode Demo Aktif/i)).toHaveCount(0);

    // 2. Select item & add item note
    const mieCard = customerPage
      .locator('div.bg-white', {
        has: customerPage.getByRole('heading', { name: 'Mie Gacoan (Pedas Manis)' }),
      })
      .first();
    await mieCard.getByRole('button', { name: 'Tambah' }).click();

    await customerPage.getByRole('button', { name: '+ Level 2' }).click();
    await customerPage.getByRole('button', { name: /Tambahkan ke Pesanan/i }).click();

    // Open cart drawer
    await customerPage.getByRole('button', { name: /Lihat Pesanan/i }).click();
    await expect(customerPage.getByText('Keranjang Pesanan')).toBeVisible();

    // Verify Take Away mandatory name validation
    await customerPage.getByRole('button', { name: /Lanjut Bayar QRIS/i }).click();
    await expect(
      customerPage.getByText(/Nama wajib diisi untuk pesanan Take Away/i)
    ).toBeVisible();

    // Fill customer name & order-level note
    await customerPage
      .getByPlaceholder(/Masukkan nama Anda untuk dipanggil di kasir/i)
      .fill(customerName);
    await customerPage
      .getByPlaceholder(/Contoh: Jangan terlalu asin/i)
      .fill('Sumpit ekstra 2 pasang');

    // Submit checkout
    await customerPage.getByRole('button', { name: /Lanjut Bayar QRIS/i }).click();

    // 3. Payment Modal -> Simulate QRIS Webhook
    await expect(customerPage.getByText('QRIS Standar Pembayaran Nasional')).toBeVisible();
    await customerPage.getByRole('button', { name: /Simulasi Bayar Berhasil/i }).click();

    // OrderStatusModal opens showing PAID and SSE active
    await expect(customerPage.getByText('Nomor Antrean Anda')).toBeVisible();
    await expect(customerPage.getByText(customerName)).toBeVisible();
    await expect(customerPage.getByText('Terhubung Real-Time (SSE)')).toBeVisible();
    await expectActiveOrderStep(customerPage, 'Lunas');
    await customerPage.screenshot({
      path: path.join(ARTIFACT_SCREENSHOT_DIR, '01-customer-paid.png'),
    });

    // 4. Open Flutter KDS Web
    await kdsPage.goto('http://127.0.0.1:3001');
    await ensureFlutterSemantics(kdsPage);

    // Verify online connection status
    await expect(
      flutterByLabelOrText(kdsPage, 'Outlet Utama • Terhubung (Auto-Sync)')
    ).toBeVisible();

    // Find our newly created order card by its semantic container label
    const orderCard = kdsPage.locator(`[aria-label*="Pesanan Take Away"][aria-label*="(${customerName})"]`).first();
    await expect(orderCard).toBeVisible({ timeout: 20_000 });
    await expect(
      orderCard.locator('[aria-label*="Catatan Pesanan: Sumpit ekstra 2 pasang"]').or(
        orderCard.getByText('Catatan Pesanan: Sumpit ekstra 2 pasang')
      ).first()
    ).toBeVisible();
    await kdsPage.screenshot({
      path: path.join(ARTIFACT_SCREENSHOT_DIR, '02-kds-order-paid.png'),
    });

    // 5. KDS: PAID -> PREPARING ("Mulai Masak")
    await orderCard.getByRole('button', { name: 'Mulai Masak' }).click();
    await expect(
      orderCard.getByRole('button', { name: 'Tandai Siap Saji' })
    ).toBeVisible({ timeout: 15_000 });

    // 6. Verify Customer Web receives SSE update to PREPARING ("Dimasak" is current step)
    await expectActiveOrderStep(customerPage, 'Dimasak');
    await customerPage.screenshot({
      path: path.join(ARTIFACT_SCREENSHOT_DIR, '03-customer-sse-preparing.png'),
    });

    // 7. KDS: Filter "Sedang Dimasak" and transition PREPARING -> READY ("Tandai Siap Saji")
    await flutterByLabelOrText(kdsPage, 'Sedang Dimasak').click();
    await expect(orderCard).toBeVisible();
    await orderCard.getByRole('button', { name: 'Tandai Siap Saji' }).click();

    // Switch to "Siap Saji" filter chip (verifies READY status alignment)
    await flutterByLabelOrText(kdsPage, 'Siap Saji').click();
    await expect(orderCard).toBeVisible({ timeout: 15_000 });
    await expect(
      orderCard.getByRole('button', { name: 'Selesai Diantar' })
    ).toBeVisible({ timeout: 15_000 });
    await kdsPage.screenshot({
      path: path.join(ARTIFACT_SCREENSHOT_DIR, '04-kds-order-ready.png'),
    });

    // 8. Verify Customer Web receives SSE update to READY ("Siap" is current step)
    await expectActiveOrderStep(customerPage, 'Siap');
    await customerPage.screenshot({
      path: path.join(ARTIFACT_SCREENSHOT_DIR, '05-customer-sse-ready.png'),
    });

    // 9. KDS: Transition READY -> COMPLETED ("Selesai Diantar")
    await orderCard.getByRole('button', { name: 'Selesai Diantar' }).click();
    await expect(orderCard).toHaveCount(0, { timeout: 15_000 });

    // 10. Verify Customer Web receives SSE update to COMPLETED ("Selesai" is current step)
    await expectActiveOrderStep(customerPage, 'Selesai');
    await customerPage.screenshot({
      path: path.join(ARTIFACT_SCREENSHOT_DIR, '06-completed.png'),
    });

    expect(customerErrors).toEqual([]);
    expect(kdsErrors).toEqual([]);
  });
});
