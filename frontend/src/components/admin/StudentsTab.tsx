import { useState } from 'react'
import { Pagination } from '../Pagination'
import { SearchBar } from '../SearchBar'
import { FormAlert } from '../ui/FormAlert'
import { Button } from '../ui/Button'
import { useAuth } from '../../lib/auth-context'
import { getErrorMessage } from '../../lib/apiError'
import { useAsyncResource } from '../../lib/useAsyncResource'
import {
  changeStudentRole,
  getAdminStudents,
  suspendStudent,
  unsuspendStudent,
  type AdminStudent,
} from '../../services/adminService'

function StudentRow({
  student,
  ownId,
  onChange,
}: {
  student: AdminStudent
  ownId?: number
  onChange: (student: AdminStudent) => void
}) {
  const [loading, setLoading] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  async function act(label: string, action: () => Promise<AdminStudent>) {
    if (loading) return
    setLoading(label)
    setError(null)
    try {
      onChange(await action())
    } catch (cause) {
      setError(getErrorMessage(cause, 'Could not update student.'))
    } finally {
      setLoading(null)
    }
  }
  const actionClass =
    '!bg-transparent !px-0 !py-0 !text-sm !text-accent-700 disabled:!text-zinc-400'
  return (
    <article
      data-testid={`student-${student.id}`}
      className="border-b border-zinc-200 py-4"
    >
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div className="min-w-0">
          <p className="font-medium text-zinc-900">{student.name}</p>
          <p className="break-all text-sm text-zinc-600">{student.email}</p>
          <div className="mt-2 flex flex-wrap gap-2 text-xs">
            {student.role === 'ADMIN' && (
              <span className="bg-accent-50 px-2 py-0.5 text-accent-800">
                Admin
              </span>
            )}
            {student.suspended && (
              <span className="bg-zinc-200 px-2 py-0.5 text-zinc-800">
                Suspended
              </span>
            )}
          </div>
        </div>
        {student.id === ownId ? (
          <span className="text-sm text-zinc-500">You</span>
        ) : (
          <div className="flex flex-wrap items-center gap-3">
            {student.role === 'ADMIN' ? (
              <>
                <Button
                  type="button"
                  fullWidth={false}
                  loading={loading === 'role'}
                  disabled={!!loading}
                  onClick={() =>
                    act('role', () => changeStudentRole(student.id, 'STUDENT'))
                  }
                  className={actionClass}
                >
                  Remove admin
                </Button>
                <Button
                  type="button"
                  fullWidth={false}
                  disabled
                  className={actionClass}
                >
                  Suspend
                </Button>
                <span className="text-xs text-zinc-500">
                  Remove admin role first
                </span>
              </>
            ) : (
              <>
                <Button
                  type="button"
                  fullWidth={false}
                  loading={loading === 'role'}
                  disabled={!!loading}
                  onClick={() =>
                    act('role', () => changeStudentRole(student.id, 'ADMIN'))
                  }
                  className={actionClass}
                >
                  Make admin
                </Button>
                <Button
                  type="button"
                  fullWidth={false}
                  loading={loading === 'suspend'}
                  disabled={!!loading}
                  onClick={() =>
                    act('suspend', () =>
                      student.suspended
                        ? unsuspendStudent(student.id)
                        : suspendStudent(student.id)
                    )
                  }
                  className={actionClass}
                >
                  {student.suspended ? 'Unsuspend' : 'Suspend'}
                </Button>
              </>
            )}
          </div>
        )}
      </div>
      {error && (
        <div className="mt-3">
          <FormAlert kind="error">{error}</FormAlert>
        </div>
      )}
    </article>
  )
}

export function StudentsTab() {
  const { student } = useAuth()
  const [q, setQ] = useState('')
  const [page, setPage] = useState(1)
  const { state, setData, reload } = useAsyncResource(
    () => getAdminStudents({ q, page }),
    [q, page]
  )
  return (
    <div>
      <SearchBar
        value={q}
        onSearch={(term) => {
          setQ(term)
          setPage(1)
        }}
      />
      {state.status === 'loading' || state.status === 'idle' ? (
        <p role="status" className="text-sm text-zinc-600">
          Loading students…
        </p>
      ) : state.status === 'error' ? (
        <FormAlert
          kind="error"
          action={{ label: 'Try again', onClick: reload }}
        >
          {getErrorMessage(state.error, 'Could not load students.')}
        </FormAlert>
      ) : (
        <>
          {state.data.items.map((item) => (
            <StudentRow
              key={item.id}
              student={item}
              ownId={student?.id}
              onChange={(changed) =>
                setData((current) => ({
                  ...current,
                  items: current.items.map((row) =>
                    row.id === changed.id ? changed : row
                  ),
                }))
              }
            />
          ))}
          <Pagination
            currentPage={state.data.meta.currentPage}
            totalPages={state.data.meta.totalPages}
            totalItems={state.data.meta.totalItems}
            onPageChange={setPage}
          />
        </>
      )}
    </div>
  )
}
