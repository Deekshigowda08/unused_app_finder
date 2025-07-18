package rikka.shizuku

import android.os.IBinder
import android.os.IInterface
import android.os.Parcel
import android.os.RemoteException
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import java.io.FileDescriptor
import java.util.Objects

/**
 * Binder wrapper to use ShizukuService#transactRemote more conveniently.
 *
 * Example:
 * ```kotlin
 * val pm = IPackageManager.Stub.asInterface(
 *     ShizukuBinderWrapper(SystemServiceHelper.getSystemService("package"))
 * )
 * pm.getInstalledPackages(0, 0)
 * ```
 */
class ShizukuBinderWrapper(
    @NonNull private val original: IBinder
) : IBinder {

    init {
        Objects.requireNonNull(original)
    }

    @Throws(RemoteException::class)
    override fun transact(
        code: Int,
        @NonNull data: Parcel,
        @Nullable reply: Parcel?,
        flags: Int
    ): Boolean {
        val atLeast13 = !Shizuku.isPreV11() && Shizuku.getVersion() >= 13
        val newData = Parcel.obtain()

        return try {
            newData.writeInterfaceToken(ShizukuApiConstants.BINDER_DESCRIPTOR)
            newData.writeStrongBinder(original)
            newData.writeInt(code)

            if (atLeast13) {
                newData.writeInt(flags)
            }

            newData.appendFrom(data, 0, data.dataSize())

            if (atLeast13) {
                Shizuku.transactRemote(newData, reply, 0)
            } else {
                Shizuku.transactRemote(newData, reply, flags)
            }

            true
        } finally {
            newData.recycle()
        }
    }

    @Nullable
    @Throws(RemoteException::class)
    override fun getInterfaceDescriptor(): String? {
        return original.interfaceDescriptor
    }

    override fun pingBinder(): Boolean {
        return original.pingBinder()
    }

    override fun isBinderAlive(): Boolean {
        return original.isBinderAlive()
    }

    @Nullable
    override fun queryLocalInterface(
         descriptor: String): IInterface? {
        return null
    }

    @Throws(RemoteException::class)
    override fun dump(@NonNull fd: FileDescriptor, @Nullable args: Array<String>?) {
        original.dump(fd, args)
    }

    @Throws(RemoteException::class)
    override fun dumpAsync(@NonNull fd: FileDescriptor, @Nullable args: Array<String>?) {
        original.dumpAsync(fd, args)
    }

    @Throws(RemoteException::class)
    override fun linkToDeath(@NonNull recipient: IBinder.DeathRecipient, flags: Int) {
        original.linkToDeath(recipient, flags)
    }

    override fun unlinkToDeath(@NonNull recipient: IBinder.DeathRecipient, flags: Int): Boolean {
        return original.unlinkToDeath(recipient, flags)
    }
}