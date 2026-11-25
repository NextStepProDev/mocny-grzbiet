import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { SettingsPage } from './SettingsPage'

const updateProfile = vi.fn()
const showToast = vi.fn()
vi.mock('../components/seo/Seo', () => ({ Seo: () => null }))
vi.mock('../api/client', () => ({
  authApi: { updateProfile: (...args: unknown[]) => updateProfile(...args) },
}))
vi.mock('../context/AuthContext', () => ({
  useAuth: () => ({
    user: {
      id: 'u1', email: 'ania@test.pl', firstName: 'Ania', lastName: 'Kowalska', phone: '500100100',
      role: 'USER', isAdmin: false, hasPassword: true, avatarUrl: null, marketingConsent: false,
    },
    refreshUser: vi.fn(), logout: vi.fn(),
  }),
}))
vi.mock('../context/ToastContext', () => ({ useToast: () => ({ showToast }) }))
vi.mock('../components/ui/AvatarCropper', () => ({
  AvatarCropper: ({ imageSrc }: { imageSrc: string }) => <div data-testid="cropper">{imageSrc}</div>,
}))
vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string) => key }),
  initReactI18next: { type: '3rdParty', init: () => {} },
}))

describe('SettingsPage — profile', () => {
  beforeEach(() => {
    updateProfile.mockReset()
    updateProfile.mockResolvedValue(undefined)
  })

  async function openProfileAndSetPhone(phone: string) {
    const user = userEvent.setup()
    render(<MemoryRouter><SettingsPage /></MemoryRouter>)
    await user.click(screen.getByRole('button', { name: /profile\.title/ }))
    const field = screen.getByLabelText('profile.phone')
    await user.clear(field)
    if (phone) await user.type(field, phone)
    await user.click(screen.getByRole('button', { name: 'profile.save' }))
  }

  it('shouldRefuseAPhoneThatIsNotNineDigits', async () => {
    // Found in manual testing: any string was saved as the phone.
    await openProfileAndSetPhone('123')

    expect(screen.getByText('validation.phoneInvalid')).toBeInTheDocument()
    expect(updateProfile).not.toHaveBeenCalled()
  })

  it('shouldSendTheDigitsWithoutSpaces', async () => {
    await openProfileAndSetPhone('600 700 800')

    await waitFor(() => expect(updateProfile).toHaveBeenCalledWith('Ania', 'Kowalska', '600700800'))
  })
})

/**
 * Found in manual testing: a photo off an iPhone could not become the avatar. Desktop Chrome reports
 * a `.heic` with no type, which the page refused as "not an image"; and where the browser cannot
 * decode HEIC, the cropper would open on an empty square.
 */
describe('SettingsPage — avatar from an iPhone', () => {
  function stubImage(decodes: boolean) {
    vi.stubGlobal('Image', class {
      onload: (() => void) | null = null
      onerror: (() => void) | null = null
      width = 3024
      height = 4032
      set src(_v: string) { queueMicrotask(() => (decodes ? this.onload?.() : this.onerror?.())) }
      get src() { return 'blob:stub' }
    })
    URL.createObjectURL = vi.fn(() => 'blob:converted')
    URL.revokeObjectURL = vi.fn()
    HTMLCanvasElement.prototype.getContext = vi.fn(() => ({
      drawImage: vi.fn(), fillRect: vi.fn(), fillStyle: '',
    })) as unknown as typeof HTMLCanvasElement.prototype.getContext
    HTMLCanvasElement.prototype.toBlob = function (cb: BlobCallback, type?: string) {
      cb(new Blob([new ArrayBuffer(1000)], { type }))
    }
  }

  beforeEach(() => showToast.mockReset())
  afterEach(() => vi.unstubAllGlobals())

  async function pick(file: File) {
    const user = userEvent.setup()
    const { container } = render(<MemoryRouter><SettingsPage /></MemoryRouter>)
    await user.upload(container.querySelector('input[type="file"]') as HTMLInputElement, file)
  }

  it('shouldOpenTheCropperForAHeicWithNoFileType', async () => {
    stubImage(true)
    await pick(new File([new ArrayBuffer(2000)], 'IMG_0412.HEIC', { type: '' }))

    expect(await screen.findByTestId('cropper')).toBeInTheDocument()
    expect(showToast).not.toHaveBeenCalled()
  })

  it('shouldExplainAHeicThisBrowserCannotOpen', async () => {
    stubImage(false)
    await pick(new File([new ArrayBuffer(2000)], 'IMG_0412.heic', { type: 'image/heic' }))

    await waitFor(() => expect(showToast).toHaveBeenCalledWith('avatar.unreadableHeic', 'error'))
    expect(screen.queryByTestId('cropper')).not.toBeInTheDocument()
  })
})
