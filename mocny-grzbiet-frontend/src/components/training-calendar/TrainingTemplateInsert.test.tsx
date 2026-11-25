import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { TrainingFormModal } from './TrainingFormModal'
import type { CreateTrainingBody, TrainingTemplate } from '../../types'

const getTrainingTemplates = vi.fn<() => Promise<TrainingTemplate[]>>()
vi.mock('../../api/admin', () => ({ adminApi: { getTrainingTemplates: () => getTrainingTemplates() } }))
vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string) => key }),
  initReactI18next: { type: '3rdParty', init: () => {} },
}))

const warmUp: TrainingTemplate = {
  id: 'tpl1', title: 'Rozgrzewka', description: '10 min rower, mobilizacja bioder',
  defaultDurationMinutes: null, attachments: [],
}

function renderForm(onSubmit: (body: CreateTrainingBody) => Promise<unknown>, canAttach = true) {
  render(
    <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
      <TrainingFormModal training={null} date="2027-03-10" onClose={vi.fn()} onSubmit={onSubmit}
        onConflictRefresh={vi.fn()} canAttach={canAttach} />
    </QueryClientProvider>,
  )
}

/**
 * Found in manual testing: the coach could build a library of templates and had no way to use one —
 * the panel said "schematy, które wstawiasz do planu" and the training form offered nothing.
 */
describe('TrainingFormModal — insert a template', () => {
  beforeEach(() => {
    getTrainingTemplates.mockReset()
    getTrainingTemplates.mockResolvedValue([warmUp])
  })

  it('shouldFillTheTitleAndDescriptionFromTheTemplate', async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined)
    renderForm(onSubmit)
    const user = userEvent.setup()

    await user.click(screen.getByRole('button', { name: /templates\.insert/ }))
    await user.click(await screen.findByRole('button', { name: /Rozgrzewka/ }))
    await user.click(screen.getByRole('button', { name: 'form.save' }))

    await waitFor(() => expect(onSubmit).toHaveBeenCalled())
    expect(onSubmit.mock.calls[0][0]).toMatchObject({
      title: 'Rozgrzewka', description: '10 min rower, mobilizacja bioder',
    })
  })

  it('shouldKeepWhatTheCoachAlreadyTyped', async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined)
    renderForm(onSubmit)
    const user = userEvent.setup()

    await user.type(screen.getByLabelText('form.title'), 'Siła')
    await user.type(screen.getByLabelText('form.description'), 'Najpierw:')
    await user.click(screen.getByRole('button', { name: /templates\.insert/ }))
    await user.click(await screen.findByRole('button', { name: /Rozgrzewka/ }))
    await user.click(screen.getByRole('button', { name: 'form.save' }))

    await waitFor(() => expect(onSubmit).toHaveBeenCalled())
    // The template adds to the description instead of wiping it, and a typed title stays.
    expect(onSubmit.mock.calls[0][0]).toMatchObject({
      title: 'Siła', description: 'Najpierw:\n\n10 min rower, mobilizacja bioder',
    })
  })

  it('shouldSayWhereTemplatesComeFromWhenThereAreNone', async () => {
    getTrainingTemplates.mockResolvedValue([])
    renderForm(vi.fn())
    const user = userEvent.setup()

    await user.click(screen.getByRole('button', { name: /templates\.insert/ }))

    expect(await screen.findByText('templates.pickEmpty')).toBeInTheDocument()
  })

  it('shouldNotOfferTemplatesToTheClient', () => {
    // Templates are the coach's library; the client's own entries have no access to it.
    renderForm(vi.fn(), false)

    expect(screen.queryByRole('button', { name: /templates\.insert/ })).toBeNull()
  })
})
