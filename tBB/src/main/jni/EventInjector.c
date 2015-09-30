/*
 * Android Event Injector 
 *
 * Copyright (c) 2013 by Radu Motisan , radu.motisan@gmail.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * For more information on the GPL, please go to:
 * http://www.gnu.org/copyleft/gpl.html
 *
 */

#include <string.h>
#include <stdint.h>
#include <jni.h>
#include <stdlib.h>
#include <unistd.h>
#include <fcntl.h>
#include <stdio.h>
#include <dirent.h>
#include <time.h>
#include <errno.h>

#include <sys/ioctl.h>
#include <sys/mman.h>
#include <sys/types.h>
#include <sys/inotify.h>
#include <sys/limits.h>
#include <sys/poll.h>

#include <linux/fb.h>
#include <linux/kd.h>
#include <input.h>
#include <uinput.h>

#include <android/log.h>
#define TAG "EventInjector::JNI"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG  , TAG, __VA_ARGS__) 
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

#include "EventInjector.h"

/* Debug tools
 */
int g_debug = 0;

void debug(char *szFormat, ...) {
	if (g_debug == 0)
		return;
	//if (strlen(szDbgfile) == 0) return;

	char szBuffer[4096]; //in this buffer we form the message
	const size_t NUMCHARS = sizeof(szBuffer) / sizeof(szBuffer[0]);
	const int LASTCHAR = NUMCHARS - 1;
	//format the input string
	va_list pArgs;
	va_start(pArgs, szFormat);
	// use a bounded buffer size to prevent buffer overruns.  Limit count to
	// character size minus one to allow for a NULL terminating character.
	vsnprintf(szBuffer, NUMCHARS - 1, szFormat, pArgs);
	va_end(pArgs);
	//ensure that the formatted string is NULL-terminated
	szBuffer[LASTCHAR] = '\0';

	//LOGD(szBuffer);
	//TextCallback(szBuffer);
}

jint Java_tbb_core_ioManager_Events_intEnableDebug(JNIEnv* env, jobject thiz,
		jint enable) {

	g_debug = enable;
	return g_debug;
}

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
	debug("eventinterceptor native lib loaded.");
	return JNI_VERSION_1_2; //1_2 1_4
}

void JNI_OnUnload(JavaVM *vm, void *reserved) {
	debug("eventinterceptor native lib unloaded.");
}

static struct typedev {
	struct pollfd ufds;
	char *device_path;
	char *device_name;
}*pDevs = NULL;
struct pollfd *ufds;
static int nDevsCount;

const char *device_path = "/dev/input";

int g_Polling = 0;
struct input_event event;
int c;
int i;
int pollres;
int get_time = 0;
char *newline = "\n";
uint16_t get_switch = 0;
struct input_event event;
int version;

int dont_block = -1;
int event_count = 0;
int sync_rate = 0;
int64_t last_sync_time = 0;
const char *device = NULL;

#define die(str, args...) do { \
        perror(str); \
        exit(EXIT_FAILURE); \
    } while(0)

int idVirtualTouch = 0;
static int startDevice(const char *touchdevice, int protocol, int absX, int absY) {

	struct uinput_user_dev uidev;
	int fd;

	fd = open("/dev/uinput", O_WRONLY | O_NONBLOCK);
	if (fd < 0) {
		die("error: open");
	}
	memset(&uidev, 0, sizeof(uidev));

	snprintf(uidev.name, UINPUT_MAX_NAME_SIZE, "%s",touchdevice);



	ioctl(fd, UI_SET_EVBIT, EV_ABS);


	ioctl(fd, UI_SET_ABSBIT, ABS_MT_POSITION_X);
	ioctl(fd, UI_SET_ABSBIT, ABS_MT_POSITION_Y);
	ioctl(fd, UI_SET_ABSBIT, ABS_MT_PRESSURE);
	ioctl(fd, UI_SET_ABSBIT, ABS_MT_TOUCH_MINOR);
	ioctl(fd, UI_SET_ABSBIT, ABS_MT_TOUCH_MAJOR);



	ioctl(fd, UI_SET_ABSBIT, ABS_MT_ORIENTATION);
	ioctl(fd, UI_SET_ABSBIT, BTN_TOOL_FINGER);
	ioctl(fd, UI_SET_ABSBIT, BTN_TOOL_DOUBLETAP);
	ioctl(fd, UI_SET_ABSBIT, BTN_TOOL_TRIPLETAP);
	ioctl(fd, UI_SET_ABSBIT, BTN_TOOL_QUADTAP);


	ioctl(fd, UI_SET_ABSBIT, ABS_MT_TRACKING_ID);
	ioctl(fd, UI_SET_ABSBIT, ABS_MT_SLOT);

	ioctl(fd, UI_SET_KEYBIT, BTN_TOUCH);

	uidev.id.bustype = 0;
	uidev.id.vendor = 0x0;
	uidev.id.product = 0x0;
	uidev.id.version = 0;
	//uidev.absmax[ABS_MT_POSITION_X] = 480;
	//uidev.absmax[ABS_MT_POSITION_Y] = 800;
	uidev.absmax[ABS_MT_POSITION_X] = absX;
	uidev.absmax[ABS_MT_POSITION_Y] = absY;

	uidev.absmax[ABS_MT_TOUCH_MAJOR] = 64;
	uidev.absmax[ABS_MT_WIDTH_MAJOR] = 64;
	uidev.absmax[ABS_MT_PRESSURE] = 64;

	if(protocol==1){
		uidev.absmax[ABS_MT_TRACKING_ID] = 65535;
		uidev.absmax[ABS_MT_SLOT] = 9;
	}

	if (write(fd, &uidev, sizeof(uidev)) < 0) {
		die("error: write");
	}

	if (ioctl(fd, UI_DEV_CREATE, 0) < 0) {
		die("error: ioctl");
	}

	idVirtualTouch = fd;

	return 55;

}

static int open_device(int index) {
	if (index >= nDevsCount || pDevs == NULL)
		return -1;
	debug("open_device prep to open");
	char *device = pDevs[index].device_path;

	debug("open_device call %s", device);
	int version;
	int fd;
	struct fb_var_screeninfo varInfo;
	struct fb_fix_screeninfo finfo;

	char name[80];
	char location[80];
	char idstr[80];
	struct input_id id;

	fd = open(device, O_RDWR);
	if (fd < 0) {
		pDevs[index].ufds.fd = -1;
		pDevs[index].device_name = NULL;
		debug("could not open %s, %s", device, strerror(errno));
		return -1;
	}

	pDevs[index].ufds.fd = fd;
	ufds[index].fd = fd;

	name[sizeof(name) - 1] = '\0';
	if (ioctl(fd, EVIOCGNAME(sizeof(name) - 1), &name) < 1) {
		debug("could not get device name for %s, %s", device, strerror(errno));
		name[0] = '\0';
	}
	pDevs[index].device_name = strdup(name);

	/*
	 if (strstr(name, "input") != NULL) {

	 int fd = open("/dev/graphics/fb0", O_RDONLY);
	 if(ioctl(fd, FBIOGET_FSCREENINFO, &finfo)==-1)
	 LOGD("eror");
	 fd = open("/dev/graphics/fb0", O_RDONLY);
	 if(ioctl(fd, FBIOGET_VSCREENINFO, &varInfo)==-1)
	 LOGD("eror");

	 return  varInfo.yres;
	 }*/
	//debug("Device %d: %s: %s", nDevsCount, device, name);
	return 0;
}

int remove_device(int index) {
	if (index >= nDevsCount || pDevs == NULL)
		return -1;

	int count = nDevsCount - index - 1;
	debug("remove device %d", index);
	free(pDevs[index].device_path);
	free(pDevs[index].device_name);

	memmove(&pDevs[index], &pDevs[index + 1], sizeof(pDevs[0]) * count);
	nDevsCount--;
	return 0;
}

static int scan_dir(const char *dirname) {
	nDevsCount = 0;
	char devname[PATH_MAX];
	char *filename;
	DIR *dir;
	struct dirent *de;
	dir = opendir(dirname);
	if (dir == NULL)
		return -1;
	strcpy(devname, dirname);
	filename = devname + strlen(devname);
	*filename++ = '/';
	while ((de = readdir(dir))) {
		if (de->d_name[0] == '.'
				&& (de->d_name[1] == '\0'
						|| (de->d_name[1] == '.' && de->d_name[2] == '\0')))
			continue;
		strcpy(filename, de->d_name);
		debug("scan_dir:prepare to open:%s", devname);
		// add new filename to our structure: devname
		struct typedev *new_pDevs = realloc(pDevs,
				sizeof(pDevs[0]) * (nDevsCount + 1));
		if (new_pDevs == NULL) {
			debug("out of memory");
			return -1;
		}
		pDevs = new_pDevs;

		struct pollfd *new_ufds = realloc(ufds,
				sizeof(ufds[0]) * (nDevsCount + 1));
		if (new_ufds == NULL) {
			debug("out of memory");
			return -1;
		}
		ufds = new_ufds;
		ufds[nDevsCount].events = POLLIN;

		pDevs[nDevsCount].ufds.events = POLLIN;
		pDevs[nDevsCount].device_path = strdup(devname);

		nDevsCount++;
	}
	closedir(dir);
	return 0;
}
jint Java_tbb_core_ioManager_Events_sendVirtualEvent(JNIEnv* env,
		jobject thiz, uint16_t type, uint16_t code, int32_t value) {

	struct input_event event;


	memset(&event, 0, sizeof(event));
	event.type = type;
	event.code = code;
	event.value = value;

	if (write(idVirtualTouch, &event, sizeof(struct input_event)) < 0)
			die("error");
}

jint Java_tbb_core_ioManager_Events_intSendEvent(JNIEnv* env, jobject thiz,
		jint index, uint16_t type, uint16_t code, int32_t value) {
	int fd = 0;
	if (index == -2)
		fd = idVirtualTouch;
	else {
		if (index >= nDevsCount || pDevs[index].ufds.fd == -1)
			return -1;
		fd = pDevs[index].ufds.fd;
	}
	debug("SendEvent call (%d,%d,%d,%d)", fd, type, code, value);
	struct uinput_event event;
	int len;

	if (fd <= fileno(stderr))
		return;

	memset(&event, 0, sizeof(event));
	event.type = type;
	event.code = code;
	event.value = value;

	len = write(fd, &event, sizeof(event));

	debug("SendEvent done:%d", len);
}

jint Java_tbb_core_ioManager_Events_ScanFiles(JNIEnv* env, jobject thiz) {
	int res = scan_dir(device_path);
	if (res < 0) {
		debug("scan dir failed for %s:", device_path);
		return -1;
	}

	return nDevsCount;
}

jstring Java_tbb_core_ioManager_Events_getDevPath(JNIEnv* env, jobject thiz,
		jint index) {
	return (*env)->NewStringUTF(env, pDevs[index].device_path);
}
jstring Java_tbb_core_ioManager_Events_getDevName(JNIEnv* env, jobject thiz,
		jint index) {
	if (pDevs[index].device_name == NULL)
		return NULL;
	else
		return (*env)->NewStringUTF(env, pDevs[index].device_name);
}

jint Java_tbb_core_ioManager_Events_getTime(JNIEnv* env, jobject thiz) {
	return event.time.tv_sec * 1000 + (event.time.tv_usec / 1000);
}

jint Java_tbb_core_ioManager_Events_OpenDev(JNIEnv* env, jobject thiz,
		jint index) {
	return open_device(index);
}
jint Java_tbb_core_ioManager_Events_takeOverDevice(JNIEnv* env, jobject thiz,
		jint index, jint control) {
	int fd = pDevs[index].ufds.fd;
	if (fd == nDevsCount)
		return -4;
	int result = ioctl(fd, EVIOCGRAB, control); //1=grab | 0=release
	return result;
}

jint Java_tbb_core_ioManager_Events_RemoveDev(JNIEnv* env, jobject thiz,
		jint index) {
	if (index == 2)
		if (ioctl(idVirtualTouch, UI_DEV_DESTROY) < 0)
			die("error: ioctl");
	return remove_device(index);
}

jint Java_tbb_core_ioManager_Events_PollDev(JNIEnv* env, jobject thiz,
		jint index) {
	if (index >= nDevsCount || pDevs[index].ufds.fd == -1)
		return -1;
	int pollres = poll(ufds, nDevsCount, -1);
	if (ufds[index].revents) {
		if (ufds[index].revents & POLLIN) {
			int res = read(ufds[index].fd, &event, sizeof(event));
			if (res < (int) sizeof(event)) {
				return 1;
			} else
				return 0;
		}
	}
	return -1;
}

jint Java_tbb_core_ioManager_Events_getType(JNIEnv* env, jobject thiz) {
	return event.type;
}

jint Java_tbb_core_ioManager_Events_getCode(JNIEnv* env, jobject thiz) {
	return event.code;
}

jint Java_tbb_core_ioManager_Events_getCode2(JNIEnv* env, jobject thiz) {
	return 0;
	//return startDevice();
}

jint Java_tbb_core_ioManager_Events_createVirtualDevice(
		JNIEnv* env, jobject thiz, jstring touchDevice, jint protocol, jint absX, jint absY) {
	//return 0;
	const char *nativeString = (*env)->GetStringUTFChars(env, touchDevice, 0);

	return startDevice(nativeString, protocol, absX, absY);
}

jint Java_tbb_core_ioManager_Events_getValue(JNIEnv* env, jobject thiz) {
	return event.value;
}

