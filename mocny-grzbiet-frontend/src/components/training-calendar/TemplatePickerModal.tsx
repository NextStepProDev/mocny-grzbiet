import { useTranslation } from 'react-i18next'
import { useQuery } from '@tanstack/react-query'
import { adminApi } from '../../api/admin'
import { Modal } from '../ui/Modal'
import { LoadingSpinner } from '../ui/LoadingSpinner'
import type { TrainingTemplate } from '../../types'
import { SHORT_STALE_MS } from '../../utils/queryFreshness'

/**
 * Picks one of the coach's templates for the training being written. The caller copies the content
 * in, so a later edit of the template never rewrites a session already handed out.
 * <p>
 * Rendered only while open, like the video picker: mounted closed it would still run its query and
 * drag a QueryClient requirement into every screen that merely CAN open it.
 */
export function TemplatePickerModal({
  onClose, onPick,
}: {
  onClose: () => void
  onPick: (template: TrainingTemplate) => void
}) {
  const { t } = useTranslation('calendar')
  // Same key as the panel's list, so a template added there a minute ago is already here.
  const templatesQuery = useQuery({
    queryKey: ['admin', 'training-templates'],
    queryFn: () => adminApi.getTrainingTemplates(),
    staleTime: SHORT_STALE_MS,
  })

  return (
    <Modal isOpen onClose={onClose} title={t('templates.pickTitle')}>
      {templatesQuery.isLoading ? (
        <LoadingSpinner size="sm" />
      ) : templatesQuery.data && templatesQuery.data.length > 0 ? (
        <ul className="max-h-80 space-y-2 overflow-y-auto pr-1">
          {templatesQuery.data.map(template => (
            <li key={template.id}>
              <button
                type="button"
                onClick={() => { onPick(template); onClose() }}
                className="block w-full rounded-xl border border-white/[0.07] bg-surface-850 p-3 text-left transition-colors hover:border-white/25"
              >
                <span className="block truncate font-medium text-surface-100">{template.title}</span>
                {template.description && (
                  <span className="mt-0.5 block truncate text-sm text-surface-400">{template.description}</span>
                )}
              </button>
            </li>
          ))}
        </ul>
      ) : (
        <p className="py-4 text-sm text-surface-500">{t('templates.pickEmpty')}</p>
      )}
    </Modal>
  )
}
